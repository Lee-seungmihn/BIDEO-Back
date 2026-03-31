window.addEventListener('load', function () {
    var workSelect = document.getElementById('workSelect');
    var entrySubmitBtn = document.getElementById('entrySubmitBtn');
    if (workSelect && entrySubmitBtn) {
        workSelect.addEventListener('change', function () {
            var selected = workSelect.value !== '';
            entrySubmitBtn.disabled = !selected;
            entrySubmitBtn.style.opacity = selected ? '1' : '0.5';
            entrySubmitBtn.style.cursor = selected ? 'pointer' : 'not-allowed';
        });
    }
});

function toggleScrap(btn) {
    btn.classList.toggle("active");
}

function openContestEntryWorkModal(button) {
    var workId = Number(button && button.dataset ? button.dataset.workId : 0);
    if (!workId || typeof window.openWorkDetailModal !== 'function') {
        return;
    }
    window.openWorkDetailModal(workId);
}

// ─── 공유 모달 ─────────────────────────────────────
(function () {
    var shareBtn = document.getElementById('contestShareBtn');
    var modal = document.getElementById('contestShareModal');
    var closeBtn = document.getElementById('contestShareCloseBtn');
    var linkInput = document.getElementById('contestShareLinkInput');
    var linkCopyBtn = document.getElementById('contestShareLinkCopyBtn');
    var searchInput = document.getElementById('contestShareSearchInput');
    var chipsContainer = document.getElementById('contestShareChips');
    var recipientList = document.getElementById('contestShareRecipientList');
    var sendBtn = document.getElementById('contestShareSendBtn');

    if (!shareBtn || !modal) return;

    var selectedRecipients = [];
    var searchCache = [];
    var searchTimer = null;
    var shareUrl = window.location.href;
    var contestTitle = document.querySelector('.cl-contest-title');
    var titleText = contestTitle ? contestTitle.textContent.trim() : '공모전';

    function escapeHtml(str) {
        if (!str) return '';
        return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    function openModal() {
        selectedRecipients = [];
        searchCache = [];
        renderChips();
        renderRecipients();
        if (searchInput) searchInput.value = '';
        if (linkInput) linkInput.value = shareUrl;
        modal.hidden = false;
        setTimeout(function () { if (searchInput) searchInput.focus(); }, 0);
    }

    function closeModal() {
        modal.hidden = true;
    }

    function renderChips() {
        if (!chipsContainer) return;
        if (!selectedRecipients.length) { chipsContainer.innerHTML = ''; return; }
        chipsContainer.innerHTML = selectedRecipients.map(function (r) {
            return '<div class="work-share-chip">'
                + '<span class="work-share-chip__text">' + escapeHtml(r.nickname) + '</span>'
                + '<button class="work-share-chip__remove" type="button" data-share-remove="' + r.id + '" aria-label="' + escapeHtml(r.nickname) + ' 삭제">'
                + '<svg viewBox="0 0 24 24" width="12" height="12" aria-hidden="true"><polyline fill="none" points="20.643 3.357 12 12 3.353 20.647" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="3"></polyline><line fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="3" x1="20.649" x2="3.354" y1="20.649" y2="3.354"></line></svg>'
                + '</button></div>';
        }).join('');
    }

    function renderRecipients() {
        if (!recipientList) return;
        if (!searchCache.length) { recipientList.innerHTML = ''; return; }
        recipientList.innerHTML = searchCache.map(function (m) {
            var isSelected = selectedRecipients.some(function (s) { return s.id === m.id; });
            var avatar = m.profileImage || '/images/BIDEO_LOGO/BIDEO_favicon.png';
            return '<button class="work-share-recipient' + (isSelected ? ' is-selected' : '') + '" type="button" data-share-recipient="' + m.id + '">'
                + '<div class="work-share-recipient__main">'
                + '<div class="work-share-recipient__avatar"><img src="' + escapeHtml(avatar) + '" alt="" style="width:100%;height:100%;object-fit:cover;border-radius:999px;"></div>'
                + '<div class="work-share-recipient__copy"><div class="work-share-recipient__username">' + escapeHtml(m.nickname) + '</div></div>'
                + '</div>'
                + '<div class="work-share-recipient__check' + (isSelected ? ' is-selected' : '') + '">'
                + (isSelected ? '<svg viewBox="0 0 24 24" width="14" height="14" aria-hidden="true"><polyline fill="none" points="21.648 5.352 9.002 17.998 2.358 11.358" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="3"></polyline></svg>' : '')
                + '</div></button>';
        }).join('');
    }

    shareBtn.addEventListener('click', openModal);
    closeBtn.addEventListener('click', closeModal);

    modal.addEventListener('click', function (e) {
        if (e.target === modal) closeModal();
    });

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && !modal.hidden) closeModal();
    });

    linkCopyBtn.addEventListener('click', function () {
        navigator.clipboard.writeText(shareUrl).then(function () {
            linkCopyBtn.textContent = '복사됨';
            setTimeout(function () { linkCopyBtn.textContent = '복사'; }, 2000);
        });
    });

    searchInput.addEventListener('input', function () {
        var keyword = this.value.trim();
        if (searchTimer) clearTimeout(searchTimer);
        if (!keyword) { searchCache = []; renderRecipients(); return; }
        searchTimer = setTimeout(function () {
            fetch('/api/messages/search-members?keyword=' + encodeURIComponent(keyword))
                .then(function (res) { return res.ok ? res.json() : []; })
                .then(function (members) {
                    searchCache = members || [];
                    if (!searchCache.length) {
                        recipientList.innerHTML = '<div class="work-share-empty"><div class="work-share-empty__title">검색 결과가 없습니다.</div><div class="work-share-empty__copy">다른 이름으로 다시 검색해보세요.</div></div>';
                        return;
                    }
                    renderRecipients();
                });
        }, 300);
    });

    recipientList.addEventListener('click', function (e) {
        var btn = e.target.closest('[data-share-recipient]');
        if (!btn) return;
        var memberId = Number(btn.dataset.shareRecipient);
        var idx = selectedRecipients.findIndex(function (r) { return r.id === memberId; });
        if (idx >= 0) {
            selectedRecipients.splice(idx, 1);
        } else {
            var member = searchCache.find(function (m) { return m.id === memberId; });
            if (member) selectedRecipients.push(member);
        }
        renderChips();
        renderRecipients();
    });

    chipsContainer.addEventListener('click', function (e) {
        var removeBtn = e.target.closest('[data-share-remove]');
        if (!removeBtn) return;
        var removeId = Number(removeBtn.dataset.shareRemove);
        selectedRecipients = selectedRecipients.filter(function (r) { return r.id !== removeId; });
        renderChips();
        renderRecipients();
    });

    sendBtn.addEventListener('click', function () {
        if (!selectedRecipients.length) {
            alert('공유할 대상을 1명 이상 선택해주세요.');
            return;
        }
        sendBtn.disabled = true;
        sendBtn.textContent = '전송중...';

        var contestMsg = '[contest]' + shareUrl + '[/contest]' + titleText;
        var sends = selectedRecipients.map(function (r) {
            return fetch('/api/messages/rooms', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ memberIds: [r.id] })
            })
            .then(function (res) { return res.ok ? res.json() : null; })
            .then(function (room) {
                if (!room) throw new Error('채팅방 생성 실패');
                return fetch('/api/messages/rooms/' + room.id + '/send', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ content: contestMsg })
                });
            });
        });

        Promise.all(sends)
            .then(function () {
                alert('공모전을 공유했습니다.');
                closeModal();
            })
            .catch(function () {
                alert('메시지 전송에 실패했습니다.');
            })
            .finally(function () {
                sendBtn.disabled = false;
                sendBtn.textContent = '보내기';
            });
    });
})();
