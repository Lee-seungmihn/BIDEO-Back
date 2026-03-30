package com.app.bideo.gallery;

import com.app.bideo.repository.gallery.GalleryDAO;
import com.app.bideo.repository.interaction.BookmarkDAO;
import com.app.bideo.repository.work.WorkDAO;
import com.app.bideo.service.common.S3FileService;
import com.app.bideo.service.gallery.GalleryService;
import com.app.bideo.service.interaction.CommentService;
import com.app.bideo.service.member.FollowService;
import com.app.bideo.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

class GalleryServiceSearchTest {

    private GalleryDAO galleryDAO;
    private GalleryService galleryService;

    @BeforeEach
    void setUp() {
        galleryDAO = Mockito.mock(GalleryDAO.class);
        galleryService = new GalleryService(
                galleryDAO,
                Mockito.mock(WorkDAO.class),
                Mockito.mock(BookmarkDAO.class),
                Mockito.mock(CommentService.class),
                Mockito.mock(FollowService.class),
                Mockito.mock(NotificationService.class),
                Mockito.mock(S3FileService.class)
        );
    }

    @Test
    void getGalleryDetailThrowsWhenGalleryDoesNotExist() {
        given(galleryDAO.findById(5L)).willReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> galleryService.getGalleryDetail(5L));
    }
}
