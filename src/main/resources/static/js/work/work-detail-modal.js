(function () {
  const DEFAULT_IMAGE = '/images/BIDEO_LOGO/BIDEO_favicon.png';
  let currentMemberId = null;
  let currentWorkDetail = null;

  function escapeHtml(value) {
    return String(value ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function formatDate(dateTime) {
    if (!dateTime) return '';
    const date = new Date(dateTime);
    if (Number.isNaN(date.getTime())) return '';
    return date.toLocaleDateString('ko-KR');
  }

  function getHeartIconMarkup(liked, size) {
    const color = liked ? 'rgb(237, 73, 86)' : '#262626';
    return '<svg viewBox="0 0 24 24" width="' + size + '" height="' + size + '" aria-hidden="true">'
      + '<path d="M16.792 3.904A4.989 4.989 0 0 1 21.5 9.122c0 3.072-2.652 4.959-5.197 7.222-2.512 2.243-3.865 3.469-4.303 3.752-.477-.309-2.143-1.823-4.303-3.752C5.141 14.072 2.5 12.167 2.5 9.122a4.989 4.989 0 0 1 4.708-5.218 4.21 4.21 0 0 1 3.675 1.941c.84 1.175.98 1.763 1.12 1.763s.278-.588 1.11-1.766a4.17 4.17 0 0 1 3.679-1.938m0-2a6.04 6.04 0 0 0-4.797 2.127 6.052 6.052 0 0 0-4.787-2.127A6.985 6.985 0 0 0 .5 9.122c0 3.61 2.55 5.827 5.015 7.97.283.246.569.494.853.747l1.027.918a44.998 44.998 0 0 0 3.518 3.018 2 2 0 0 0 2.174 0 45.263 45.263 0 0 0 3.626-3.115l.922-.824c.293-.26.59-.519.885-.774 2.334-2.025 4.98-4.32 4.98-7.94a6.985 6.985 0 0 0-6.708-7.218Z" fill="' + color + '" stroke="' + color + '"></path>'
      + '</svg>';
  }

  function syncLikeButton(button, liked) {
    if (!button) return;
    button.classList.toggle('is-liked', liked);
    button.setAttribute('aria-pressed', liked ? 'true' : 'false');
    button.innerHTML = getHeartIconMarkup(liked, 24);
  }

  function renderMedia(work) {
    const wrap = document.getElementById('workDetailMediaWrap');
    if (!wrap) return;
    const file = work?.files?.[0];
    const mediaUrl = file?.fileUrl || work?.thumbnailUrl || DEFAULT_IMAGE;
    const fileType = file?.fileType || '';

    if (fileType.startsWith('video/') || work?.category === 'VIDEO') {
      wrap.innerHTML = '<video class="work-detail-media" src="' + mediaUrl + '" controls autoplay muted loop playsinline></video>';
      return;
    }
    wrap.innerHTML = '<img class="work-detail-media" src="' + mediaUrl + '" alt="작품 상세">';
  }

  function renderComments(comments) {
    const container = document.getElementById('workDetailCommentsContainer');
    if (!container) return;
    container.innerHTML = (comments || []).map(function (comment) {
      const likeCount = Number(comment.likeCount ?? 0).toLocaleString('ko-KR');
      return '<div class="work-detail-comment-item">'
        + '<img class="work-detail-comment-avatar" src="' + escapeHtml(comment.memberProfileImage || DEFAULT_IMAGE) + '" alt="">'
        + '<div class="work-detail-comment-copy">'
        + '<div class="work-detail-comment-body">'
        + '<span class="work-detail-comment-name">' + escapeHtml(comment.memberNickname || 'user') + '</span>'
        + '<span class="work-detail-comment-text">' + escapeHtml(comment.content || '') + '</span>'
        + '</div>'
        + '<div class="work-detail-comment-meta"><span>' + escapeHtml(formatDate(comment.createdDatetime)) + '</span></div>'
        + '</div>'
        + '<div class="work-detail-comment-like-wrap">'
        + '<button class="work-detail-comment-like-btn' + (comment.isLiked ? ' is-liked' : '') + '" type="button" data-comment-id="' + comment.id + '" aria-label="좋아요" aria-pressed="' + (comment.isLiked ? 'true' : 'false') + '">' + getHeartIconMarkup(Boolean(comment.isLiked), 12) + '</button>'
        + '<span class="work-detail-comment-like-count">' + likeCount + '</span>'
        + '</div>'
        + '</div>';
    }).join('');
  }

  function renderWork(work) {
    currentWorkDetail = work;
    document.getElementById('workDetailAvatar').src = work.memberProfileImage || DEFAULT_IMAGE;
    document.getElementById('workDetailAuthor').textContent = work.memberNickname || '작성자';
    document.getElementById('workDetailTitle').textContent = work.title || '';
    document.getElementById('workDetailDescription').textContent = work.description || '';
    document.getElementById('workDetailViewCount').textContent = '조회수 ' + Number(work.viewCount ?? 0).toLocaleString('ko-KR') + '회';
    document.getElementById('workDetailDate').textContent = formatDate(work.createdDatetime);
    document.getElementById('workDetailLikeCount').textContent = Number(work.likeCount ?? 0).toLocaleString('ko-KR');
    document.getElementById('workDetailCommentSummary').textContent = '댓글 ' + Number(work.commentCount ?? 0).toLocaleString('ko-KR') + '개';
    document.getElementById('workDetailPrice').textContent = work.price ? Number(work.price).toLocaleString('ko-KR') + '원' : '';
    const tags = (work.tags || []).map(function (tag) {
      return '<span class="work-detail-tag-chip">#' + escapeHtml(String(tag.tagName || '').replace(/^#/, '')) + '</span>';
    }).join('');
    document.getElementById('workDetailTags').innerHTML = tags;
    syncLikeButton(document.getElementById('workDetailLikeButton'), Boolean(work.isLiked));
    document.getElementById('workDetailSavedButton')?.classList.toggle('is-saved', Boolean(work.isBookmarked));
    renderMedia(work);
    renderComments(work.comments || []);
    document.getElementById('workDetailModal')?.classList.add('active');
  }

  async function loadCurrentMember() {
    try {
      const response = await fetch('/api/auth/me');
      if (!response.ok) return;
      const member = await response.json();
      currentMemberId = member?.id ?? null;
    } catch (e) {
      currentMemberId = null;
    }
  }

  async function refreshWorkDetail() {
    if (!currentWorkDetail?.id) return;
    const response = await fetch('/api/works/' + currentWorkDetail.id);
    if (!response.ok) throw new Error('작품 정보를 불러오지 못했습니다.');
    const work = await response.json();
    renderWork(work);
  }

  window.openWorkDetailModal = async function (workId) {
    try {
      await fetch('/api/works/' + workId + '/views', { method: 'POST' });
      const response = await fetch('/api/works/' + workId);
      if (!response.ok) throw new Error('작품 정보를 불러오지 못했습니다.');
      renderWork(await response.json());
    } catch (error) {
      alert(error.message || '작품 정보를 불러오지 못했습니다.');
    }
  };

  window.closeWorkDetailModal = function (event) {
    const modal = document.getElementById('workDetailModal');
    if (!modal) return;
    if (!event || event.target === modal) {
      modal.classList.remove('active');
    }
  };

  document.addEventListener('DOMContentLoaded', function () {
    loadCurrentMember();

    document.getElementById('workDetailCommentSubmit')?.addEventListener('click', async function () {
      if (!currentWorkDetail?.id) return;
      if (!window.IS_LOGGED_IN) {
        alert('로그인이 필요합니다.');
        return;
      }
      const input = document.getElementById('workDetailCommentInput');
      const content = input?.value.trim();
      if (!content) return;
      try {
        const response = await fetch('/api/works/' + currentWorkDetail.id + '/comments', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ targetType: 'WORK', targetId: currentWorkDetail.id, content: content })
        });
        if (!response.ok) throw new Error(await response.text());
        input.value = '';
        renderWork(await response.json());
      } catch (error) {
        alert(error.message || '댓글 등록에 실패했습니다.');
      }
    });

    document.getElementById('workDetailLikeButton')?.addEventListener('click', async function () {
      if (!currentWorkDetail?.id) return;
      if (!window.IS_LOGGED_IN) {
        alert('로그인이 필요합니다.');
        return;
      }
      try {
        const response = await fetch('/api/works/' + currentWorkDetail.id + '/likes', { method: 'POST' });
        if (!response.ok) throw new Error('좋아요 처리에 실패했습니다.');
        await refreshWorkDetail();
      } catch (error) {
        alert(error.message || '좋아요 처리에 실패했습니다.');
      }
    });

    document.getElementById('workDetailSavedButton')?.addEventListener('click', async function () {
      if (!currentWorkDetail?.id) return;
      if (!window.IS_LOGGED_IN) {
        alert('로그인이 필요합니다.');
        return;
      }
      try {
        const response = await fetch('/api/bookmarks', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ targetType: 'WORK', targetId: currentWorkDetail.id })
        });
        if (!response.ok) throw new Error('찜 처리에 실패했습니다.');
        await refreshWorkDetail();
      } catch (error) {
        alert(error.message || '찜 처리에 실패했습니다.');
      }
    });

    document.getElementById('workDetailShareButton')?.addEventListener('click', async function () {
      const shareUrl = window.location.href;
      try {
        await navigator.clipboard.writeText(shareUrl);
        alert('링크를 복사했습니다.');
      } catch (e) {
        alert('링크 복사에 실패했습니다.');
      }
    });
  });
})();
