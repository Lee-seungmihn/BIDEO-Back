function initCustomerServicePage() {
    const inquiryModal = document.getElementById("inquiry-modal");
    const inquiryOpenButton = document.querySelector("[data-open-inquiry]");
    const inquiryCloseButtons = document.querySelectorAll("[data-close-inquiry]");
    const inquirySubmitButton = document.querySelector("[data-submit-inquiry]");
    const inquiryTextarea = document.getElementById("inquiry-textarea");
    const inquiryCount = document.getElementById("inquiry-count");
    const successToast = document.getElementById("success-toast");
    let successToastTimer = null;

    if (inquiryModal && inquiryOpenButton && !inquiryOpenButton.dataset.bound) {
        const openInquiryModal = () => {
            inquiryModal.hidden = false;
            document.body.style.overflow = "hidden";
            if (inquiryTextarea) {
                inquiryTextarea.focus();
            }
        };

        const closeInquiryModal = () => {
            inquiryModal.hidden = true;
            document.body.style.overflow = "";
            inquiryOpenButton.focus();
        };

        const showSuccessToast = () => {
            inquiryModal.hidden = true;
            document.body.style.overflow = "";

            if (inquiryTextarea) {
                inquiryTextarea.value = "";
            }

            if (inquiryCount) {
                inquiryCount.textContent = "0";
            }

            if (!successToast) {
                inquiryOpenButton.focus();
                return;
            }

            successToast.hidden = false;

            if (successToastTimer) {
                window.clearTimeout(successToastTimer);
            }

            successToastTimer = window.setTimeout(() => {
                successToast.hidden = true;
                inquiryOpenButton.focus();
            }, 3000);
        };

        inquiryOpenButton.dataset.bound = "true";
        inquiryOpenButton.addEventListener("click", openInquiryModal);

        inquiryCloseButtons.forEach((button) => {
            button.addEventListener("click", closeInquiryModal);
        });

        if (inquirySubmitButton) {
            inquirySubmitButton.addEventListener("click", () => {
                const content = inquiryTextarea ? inquiryTextarea.value.trim() : "";
                if (!content) {
                    alert("내용을 입력해주세요.");
                    return;
                }

                fetch("/api/customerservice", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    credentials: "include",
                    body: JSON.stringify({ category: "일반문의", content: content })
                })
                .then((res) => {
                    if (res.ok) {
                        showSuccessToast();
                    } else {
                        alert("문의 등록에 실패했습니다.");
                    }
                })
                .catch(() => {
                    alert("문의 등록에 실패했습니다.");
                });
            });
        }

        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && !inquiryModal.hidden) {
                closeInquiryModal();
            }
        });
    }

    if (inquiryTextarea && inquiryCount && !inquiryTextarea.dataset.bound) {
        const syncCount = () => {
            inquiryCount.textContent = String(inquiryTextarea.value.length);
        };

        inquiryTextarea.dataset.bound = "true";
        syncCount();
        inquiryTextarea.addEventListener("input", syncCount);
    }
}

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initCustomerServicePage);
} else {
    initCustomerServicePage();
}
