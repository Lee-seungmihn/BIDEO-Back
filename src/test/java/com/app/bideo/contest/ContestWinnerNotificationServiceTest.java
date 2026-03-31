package com.app.bideo.contest;

import com.app.bideo.dto.contest.ContestWinnerNotificationDTO;
import com.app.bideo.mapper.contest.ContestMapper;
import com.app.bideo.service.common.S3FileService;
import com.app.bideo.service.contest.ContestService;
import com.app.bideo.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@SpringBootTest
class ContestWinnerNotificationServiceTest {

    private ContestMapper contestMapper;
    private NotificationService notificationService;
    private ContestService contestService;

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
    void dispatchWinnerNotificationsMarksContestAsSentAfterCreatingNotification() {
        given(contestMapper.selectPendingWinnerNotifications(anyLong()))
                .willReturn(List.of(
                        ContestWinnerNotificationDTO.builder()
                                .contestId(51L)
                                .contestOwnerId(7L)
                                .winnerMemberId(88L)
                                .winnerWorkId(300L)
                                .build()
                ));

        contestService.dispatchWinnerNotifications();

        verify(notificationService).createNotification(88L, 7L, "CONTEST_WIN", "CONTEST", 51L, "공모전 우승작으로 선정되었습니다.");
        verify(contestMapper).markWinnerNotificationSent(51L);
    }
}
