package com.app.bideo.gallery;

import com.app.bideo.repository.gallery.GalleryDAO;
import com.app.bideo.repository.work.WorkDAO;
import com.app.bideo.service.gallery.GalleryService;
import com.app.bideo.service.interaction.CommentService;
import com.app.bideo.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertThrows;

class GalleryServiceInteractionTest {

    private GalleryService galleryService;

    @BeforeEach
    void setUp() {
        galleryService = new GalleryService(
                Mockito.mock(GalleryDAO.class),
                Mockito.mock(WorkDAO.class),
                Mockito.mock(CommentService.class),
                Mockito.mock(NotificationService.class)
        );
    }

    @Test
    void writeCommentRejectsBlankContent() {
        assertThrows(IllegalArgumentException.class, () -> galleryService.writeComment(9L, 3L, "   "));
    }
}
