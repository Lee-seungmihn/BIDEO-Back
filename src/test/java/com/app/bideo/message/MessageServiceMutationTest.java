package com.app.bideo.message;

import com.app.bideo.domain.message.MessageVO;
import com.app.bideo.repository.member.MemberRepository;
import com.app.bideo.repository.message.MessageDAO;
import com.app.bideo.repository.message.MessageRoomDAO;
import com.app.bideo.service.message.MessageService;
import com.app.bideo.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageServiceMutationTest {

    private MessageDAO messageDAO;
    private MessageRoomDAO messageRoomDAO;
    private MemberRepository memberRepository;
    private SimpMessagingTemplate messagingTemplate;
    private NotificationService notificationService;
    private MessageService messageService;

    @BeforeEach
    void setUp() {
        messageDAO = Mockito.mock(MessageDAO.class);
        messageRoomDAO = Mockito.mock(MessageRoomDAO.class);
        memberRepository = Mockito.mock(MemberRepository.class);
        messagingTemplate = Mockito.mock(SimpMessagingTemplate.class);
        notificationService = Mockito.mock(NotificationService.class);
        messageService = new MessageService(messageDAO, messageRoomDAO, memberRepository, messagingTemplate, notificationService);
    }

    @Test
    void updateMessageRejectsMessagesOutsideEditableWindow() {
        when(messageRoomDAO.isMember(5L, 1L)).thenReturn(true);
        when(messageDAO.findById(10L)).thenReturn(Optional.of(
                MessageVO.builder()
                        .id(10L)
                        .messageRoomId(5L)
                        .senderId(1L)
                        .content("원본")
                        .createdDatetime(LocalDateTime.now().minusMinutes(6))
                        .build()
        ));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> messageService.updateMessage(5L, 10L, 1L, "수정")
        );

        assertEquals("메시지는 전송 후 5분 이내에만 수정할 수 있습니다.", exception.getMessage());
        verify(messageDAO, never()).updateContent(eq(10L), anyString());
    }

    @Test
    void toggleMessageLikeRejectsDeletedMessages() {
        when(messageRoomDAO.isMember(5L, 2L)).thenReturn(true);
        when(messageDAO.findById(11L)).thenReturn(Optional.of(
                MessageVO.builder()
                        .id(11L)
                        .messageRoomId(5L)
                        .senderId(1L)
                        .deletedDatetime(LocalDateTime.now())
                        .build()
        ));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> messageService.toggleMessageLike(5L, 11L, 2L)
        );

        assertEquals("삭제된 메시지에는 좋아요를 누를 수 없습니다.", exception.getMessage());
        verify(messageDAO, never()).saveLike(2L, 11L);
    }

    @Test
    void toggleMessageLikeCreatesMessageNotificationForReceiver() {
        when(messageRoomDAO.isMember(5L, 2L)).thenReturn(true);
        when(messageDAO.findById(12L)).thenReturn(Optional.of(
                MessageVO.builder()
                        .id(12L)
                        .messageRoomId(5L)
                        .senderId(7L)
                        .content("원본")
                        .build()
        ));
        when(messageDAO.existsLike(2L, 12L)).thenReturn(false);
        when(messageDAO.findResponseById(12L, 2L)).thenReturn(Optional.of(
                com.app.bideo.dto.message.MessageResponseDTO.builder()
                        .id(12L)
                        .messageRoomId(5L)
                        .likeCount(1)
                        .isLiked(true)
                        .build()
        ));

        messageService.toggleMessageLike(5L, 12L, 2L);

        verify(notificationService).createNotification(
                7L,
                2L,
                "LIKE",
                "MESSAGE",
                12L,
                "메시지에 좋아요를 눌렀습니다."
        );
    }
}
