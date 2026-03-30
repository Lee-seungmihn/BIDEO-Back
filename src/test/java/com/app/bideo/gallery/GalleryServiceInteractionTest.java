package com.app.bideo.gallery;

import com.app.bideo.dto.gallery.GalleryDetailResponseDTO;
import com.app.bideo.repository.gallery.GalleryDAO;
import com.app.bideo.repository.work.WorkDAO;
import com.app.bideo.service.gallery.GalleryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

class GalleryServiceInteractionTest {

    private GalleryDAO galleryDAO;
    private GalleryService galleryService;

//    @BeforeEach
//    void setUp() {
//        galleryDAO = Mockito.mock(GalleryDAO.class);
//        galleryService = new GalleryService(galleryDAO, Mockito.mock(WorkDAO.class));
//    }

    @Test
    void writeCommentRejectsWhenGalleryDisablesComments() {
        given(galleryDAO.findMemberIdById(9L)).willReturn(Optional.of(1L));
        given(galleryDAO.findById(9L)).willReturn(Optional.of(
                GalleryDetailResponseDTO.builder()
                        .id(9L)
                        .memberId(1L)
                        .allowComment(false)
                        .build()
        ));

        assertThrows(IllegalStateException.class, () -> galleryService.writeComment(9L, 3L, "안녕하세요"));
    }
}
