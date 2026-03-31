package com.app.bideo.contest;

import com.app.bideo.dto.contest.ContestDetailResponseDTO;
import com.app.bideo.dto.contest.ContestEntryRequestDTO;
import com.app.bideo.mapper.contest.ContestMapper;
import com.app.bideo.service.common.S3FileService;
import com.app.bideo.service.contest.ContestService;
import com.app.bideo.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

@SpringBootTest
class ContestParticipationServiceTest {

    private ContestMapper contestMapper;
    private ContestService contestService;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        contestMapper = Mockito.mock(ContestMapper.class);
        notificationService = Mockito.mock(NotificationService.class);
        contestService = new ContestService(
                contestMapper,
                notificationService,
                Mockito.mock(S3FileService.class)
        );
    }

    @Test
    void submitEntryRejectsWorkThatDoesNotBelongToMember() {
        ContestEntryRequestDTO requestDTO = ContestEntryRequestDTO.builder()
                .contestId(10L)
                .workId(55L)
                .build();
        given(contestMapper.existsContest(10L)).willReturn(true);
        given(contestMapper.selectContestDetail(10L, null)).willReturn(openContest(10L, 99L));
        given(contestMapper.existsOwnedWork(7L, 55L)).willReturn(false);

        assertThrows(IllegalArgumentException.class, () -> contestService.submitEntry(7L, requestDTO));
    }

    @Test
    void submitEntryRejectsDuplicateContestWorkPair() {
        ContestEntryRequestDTO requestDTO = ContestEntryRequestDTO.builder()
                .contestId(10L)
                .workId(55L)
                .build();
        given(contestMapper.existsContest(10L)).willReturn(true);
        given(contestMapper.selectContestDetail(10L, null)).willReturn(openContest(10L, 99L));
        given(contestMapper.existsOwnedWork(7L, 55L)).willReturn(true);
        given(contestMapper.existsContestEntry(10L, 55L)).willReturn(true);

        assertThrows(IllegalStateException.class, () -> contestService.submitEntry(7L, requestDTO));
    }

    @Test
    void submitEntryCreatesEntryAndUpdatesEntryCount() {
        ContestEntryRequestDTO requestDTO = ContestEntryRequestDTO.builder()
                .contestId(10L)
                .workId(55L)
                .build();
        given(contestMapper.existsContest(10L)).willReturn(true);
        given(contestMapper.selectContestDetail(10L, null)).willReturn(openContest(10L, 99L));
        given(contestMapper.existsOwnedWork(7L, 55L)).willReturn(true);
        given(contestMapper.existsContestEntry(10L, 55L)).willReturn(false);

        assertDoesNotThrow(() -> contestService.submitEntry(7L, requestDTO));

        verify(contestMapper).insertContestEntry(7L, requestDTO);
        verify(contestMapper).increaseContestEntryCount(10L);
    }

    @Test
    void submitEntryRejectsWhenContestEntryPeriodHasEnded() {
        ContestEntryRequestDTO requestDTO = ContestEntryRequestDTO.builder()
                .contestId(10L)
                .workId(55L)
                .build();
        given(contestMapper.existsContest(10L)).willReturn(true);
        given(contestMapper.selectContestDetail(10L, null)).willReturn(closedContest(10L, 99L));

        assertThrows(IllegalArgumentException.class, () -> contestService.submitEntry(7L, requestDTO));
    }

    @Test
    void selectWinnerMarksOnlyChosenEntryAfterContestCloses() {
        given(contestMapper.selectContestDetail(10L, null)).willReturn(closedContest(10L, 7L));
        given(contestMapper.existsContestEntryById(10L, 200L)).willReturn(true);
        given(contestMapper.updateContestWinner(10L, 200L, "우승")).willReturn(1);

        assertDoesNotThrow(() -> contestService.selectWinner(10L, 7L, 200L));

        verify(contestMapper).clearContestWinner(10L);
        verify(contestMapper).updateContestWinner(10L, 200L, "우승");
    }

    @Test
    void selectWinnerRejectsWhenWinnerNotificationAlreadySent() {
        ContestDetailResponseDTO detail = closedContest(10L, 7L);
        detail.setWinnerNotifiedAt(LocalDateTime.of(2026, 4, 5, 0, 5));
        given(contestMapper.selectContestDetail(10L, null)).willReturn(detail);

        assertThrows(IllegalStateException.class, () -> contestService.selectWinner(10L, 7L, 200L));
    }

    @Test
    void dispatchWinnerNotificationsCreatesContestWinnerNotificationOnce() {
        given(contestMapper.selectPendingWinnerNotifications(anyLong()))
                .willReturn(java.util.List.of(
                        com.app.bideo.dto.contest.ContestWinnerNotificationDTO.builder()
                                .contestId(10L)
                                .contestOwnerId(7L)
                                .winnerMemberId(21L)
                                .winnerWorkId(200L)
                                .build()
                ));

        contestService.dispatchWinnerNotifications();

        verify(notificationService).createNotification(21L, 7L, "CONTEST_WIN", "CONTEST", 10L, "공모전 우승작으로 선정되었습니다.");
        verify(contestMapper).markWinnerNotificationSent(10L);
    }

    private ContestDetailResponseDTO openContest(Long contestId, Long ownerId) {
        return ContestDetailResponseDTO.builder()
                .id(contestId)
                .memberId(ownerId)
                .entryStart(LocalDate.now().minusDays(2))
                .entryEnd(LocalDate.now().plusDays(2))
                .resultDate(LocalDate.now().plusDays(5))
                .build();
    }

    private ContestDetailResponseDTO closedContest(Long contestId, Long ownerId) {
        return ContestDetailResponseDTO.builder()
                .id(contestId)
                .memberId(ownerId)
                .entryStart(LocalDate.now().minusDays(10))
                .entryEnd(LocalDate.now().minusDays(1))
                .resultDate(LocalDate.now().plusDays(3))
                .build();
    }
}
