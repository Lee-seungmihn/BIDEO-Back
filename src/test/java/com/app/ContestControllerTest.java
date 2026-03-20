package com.app;

import com.app.bideo.auth.member.CustomUserDetails;
import com.app.bideo.common.enumeration.MemberRole;
import com.app.bideo.common.enumeration.MemberStatus;
import com.app.bideo.controller.contest.ContestController;
import com.app.bideo.domain.member.MemberVO;
import com.app.bideo.dto.common.PageResponseDTO;
import com.app.bideo.dto.contest.ContestCreateRequestDTO;
import com.app.bideo.dto.contest.ContestListResponseDTO;
import com.app.bideo.service.contest.ContestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ContestController.class)
@AutoConfigureMockMvc(addFilters = false)
class ContestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContestService contestService;

    @Test
    void registerPageUsesContestRegisterTemplateWithEmptyFormObject() throws Exception {
        mockMvc.perform(get("/contest/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("contest/contest-register"))
                .andExpect(model().attributeExists("contestForm"));
    }

    @Test
    void createContestRedirectsToDetailForAuthenticatedMember() throws Exception {
        given(contestService.createContest(eq(7L), any(ContestCreateRequestDTO.class))).willReturn(31L);

        mockMvc.perform(post("/contest/register")
                        .with(authentication(authenticatedUser(7L)))
                        .param("title", "봄 공모전")
                        .param("organizer", "BIDEO")
                        .param("category", "영상")
                        .param("region", "서울")
                        .param("description", "설명")
                        .param("coverImage", "https://example.com/poster.png")
                        .param("entryStart", "2026-03-20")
                        .param("entryEnd", "2026-03-31")
                        .param("resultDate", "2026-04-10")
                        .param("prizeInfo", "100만원")
                        .param("price", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/contest/detail/31"));

        verify(contestService).createContest(eq(7L), any(ContestCreateRequestDTO.class));
    }

    @Test
    void myContestsUsesHostedContestTemplateAndModel() throws Exception {
        given(contestService.getHostedContestList(7L)).willReturn(pageResponse());

        mockMvc.perform(get("/contest/my-contests").with(authentication(authenticatedUser(7L))))
                .andExpect(status().isOk())
                .andExpect(view().name("contest/contestlist"))
                .andExpect(model().attributeExists("contestList"));

        verify(contestService).getHostedContestList(7L);
    }

    @Test
    void myEntriesUsesParticipatedContestTemplateAndModel() throws Exception {
        given(contestService.getParticipatedContestList(7L)).willReturn(pageResponse());

        mockMvc.perform(get("/contest/my-entries").with(authentication(authenticatedUser(7L))))
                .andExpect(status().isOk())
                .andExpect(view().name("contest/mycontests"))
                .andExpect(model().attributeExists("contestList"));

        verify(contestService).getParticipatedContestList(7L);
    }

    private PageResponseDTO<ContestListResponseDTO> pageResponse() {
        return PageResponseDTO.<ContestListResponseDTO>builder()
                .content(List.of(ContestListResponseDTO.builder().id(1L).title("테스트").build()))
                .page(1)
                .size(10)
                .totalElements(1L)
                .totalPages(1)
                .build();
    }

    private UsernamePasswordAuthenticationToken authenticatedUser(Long memberId) {
        MemberVO member = MemberVO.builder()
                .id(memberId)
                .email("contest@test.com")
                .password("pw")
                .nickname("contest-user")
                .role(MemberRole.USER)
                .status(MemberStatus.ACTIVE)
                .build();
        CustomUserDetails principal = new CustomUserDetails(member);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
