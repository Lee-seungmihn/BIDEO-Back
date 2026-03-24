package com.app.bideo.service.gallery;

import com.app.bideo.auth.member.CustomUserDetails;
import com.app.bideo.domain.interaction.CommentVO;
import com.app.bideo.dto.gallery.GalleryCreateRequestDTO;
import com.app.bideo.dto.gallery.GalleryListResponseDTO;
import com.app.bideo.dto.gallery.GalleryUpdateRequestDTO;
import com.app.bideo.dto.interaction.CommentResponseDTO;
import com.app.bideo.repository.gallery.GalleryDAO;
import com.app.bideo.repository.work.WorkDAO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class GalleryService {

    private final GalleryDAO galleryDAO;
    private final WorkDAO workDAO;

    // 예술관 등록
    public void write(Long memberId, GalleryCreateRequestDTO requestDTO, MultipartFile coverFile) {
        requestDTO.setMemberId(resolveMemberId(memberId));
        requestDTO.setCoverImage(saveCoverImage(coverFile));
        requestDTO.setAllowComment(requestDTO.getAllowComment() != null ? requestDTO.getAllowComment() : true);
        requestDTO.setShowSimilar(requestDTO.getShowSimilar() != null ? requestDTO.getShowSimilar() : true);
        galleryDAO.save(requestDTO);
    }

    // 프로필 하이라이트용 예술관 목록 조회
    @Transactional(readOnly = true)
    public List<GalleryListResponseDTO> getProfileGalleries() {
        return galleryDAO.findAllByMemberId(resolveMemberId(null));
    }

    public void update(Long id, Long memberId, GalleryUpdateRequestDTO requestDTO, MultipartFile coverFile) {
        Long resolvedMemberId = resolveMemberId(memberId);
        validateGalleryOwner(id, resolvedMemberId);
        if (requestDTO.getTitle() == null || requestDTO.getTitle().trim().isBlank()) {
            throw new IllegalArgumentException("gallery title is required");
        }

        requestDTO.setTitle(requestDTO.getTitle().trim());
        requestDTO.setDescription(requestDTO.getDescription() == null ? "" : requestDTO.getDescription().trim());

        if (coverFile != null && !coverFile.isEmpty()) {
            requestDTO.setCoverImage(saveCoverImage(coverFile));
        }

        galleryDAO.update(id, requestDTO);
    }

    public void delete(Long id, Long memberId) {
        Long resolvedMemberId = resolveMemberId(memberId);
        validateGalleryOwner(id, resolvedMemberId);
        galleryDAO.delete(id);
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDTO> getComments(Long id) {
        galleryDAO.findMemberIdById(id)
                .orElseThrow(() -> new IllegalArgumentException("gallery not found"));
        return galleryDAO.findCommentsByGalleryId(id);
    }

    public List<CommentResponseDTO> writeComment(Long galleryId, Long memberId, String content) {
        Long resolvedMemberId = resolveMemberId(memberId);
        galleryDAO.findMemberIdById(galleryId)
                .orElseThrow(() -> new IllegalArgumentException("gallery not found"));

        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedContent.isBlank()) {
            throw new IllegalArgumentException("comment content is empty");
        }

        galleryDAO.saveComment(
                CommentVO.builder()
                        .memberId(resolvedMemberId)
                        .targetType("GALLERY")
                        .targetId(galleryId)
                        .content(normalizedContent)
                        .isPinned(false)
                        .likeCount(0)
                        .build()
        );
        galleryDAO.increaseCommentCount(galleryId);
        return galleryDAO.findCommentsByGalleryId(galleryId);
    }

    private void validateGalleryOwner(Long galleryId, Long memberId) {
        Long ownerId = galleryDAO.findMemberIdById(galleryId)
                .orElseThrow(() -> new IllegalArgumentException("gallery not found"));
        if (!ownerId.equals(memberId)) {
            throw new IllegalStateException("forbidden");
        }
    }

    private Long resolveMemberId(Long memberId) {
        if (memberId != null) {
            return memberId;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getId();
        }

        throw new IllegalStateException("login required");
    }

    private String saveCoverImage(MultipartFile coverFile) {
        if (coverFile == null || coverFile.isEmpty()) {
            throw new IllegalArgumentException("cover image is required");
        }

        if (coverFile.getContentType() == null || !coverFile.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("image file only");
        }

        try {
            String contentType = coverFile.getContentType() != null ? coverFile.getContentType() : "image/png";
            String base64 = Base64.getEncoder().encodeToString(coverFile.getBytes());
            return "data:" + contentType + ";base64," + base64;
        } catch (IOException e) {
            throw new RuntimeException("gallery image upload failed", e);
        }
    }
}
