package com.campus.integration;

import com.campus.common.utils.UserContext;
import com.campus.course.dto.CourseDetailVO;
import com.campus.course.dto.CourseVO;
import com.campus.course.service.CourseService;
import com.campus.feedback.dto.FeedbackStatsDTO;
import com.campus.feedback.service.FeedbackService;
import com.campus.my.dto.MyItemVO;
import com.campus.my.service.MyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("API 集成测试")
class ControllerIntegrationTest {

    private MockMvc mockMvc;
    private CourseService courseService;
    private FeedbackService feedbackService;
    private MyService myService;
    private static final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        courseService = Mockito.mock(CourseService.class);
        feedbackService = Mockito.mock(FeedbackService.class);
        myService = Mockito.mock(MyService.class);

        // 设置 UserContext ThreadLocal（模拟 JwtAuthenticationFilter）
        UserContext.setUserId(TEST_USER_ID);

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new com.campus.course.controller.CourseController(courseService),
                        new com.campus.feedback.controller.FeedbackController(feedbackService),
                        new com.campus.my.controller.MyController(myService))
                .build();
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Nested @DisplayName("课程 API - GET /api/courses")
    class CourseApi {
        @Test @DisplayName("返回课程列表 200")
        void shouldListCourses() throws Exception {
            CourseVO vo = new CourseVO();
            vo.setId(1L); vo.setTitle("Java入门"); vo.setLikeCount(10);
            var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<CourseVO>(1, 12, 1);
            page.setRecords(List.of(vo));
            when(courseService.listCourses(any(), anyInt(), anyInt(), any())).thenReturn(page);

            mockMvc.perform(get("/api/courses"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test @DisplayName("返回课程详情 200")
        void shouldGetDetail() throws Exception {
            CourseDetailVO vo = new CourseDetailVO();
            vo.setId(1L); vo.setTitle("Java入门"); vo.setChapters(List.of());
            when(courseService.getDetail(eq(1L), any())).thenReturn(vo);

            mockMvc.perform(get("/api/courses/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("Java入门"));
        }

        @Test @DisplayName("点赞课程返回 liked=true")
        void shouldToggleLike() throws Exception {
            when(courseService.toggleLike(eq(1L), anyLong())).thenReturn(true);

            mockMvc.perform(post("/api/courses/1/like"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.liked").value(true));
        }
    }

    @Nested @DisplayName("反馈 API - POST /api/feedback")
    class FeedbackApi {
        @Test @DisplayName("提交反馈返回 like")
        void shouldToggleFeedback() throws Exception {
            when(feedbackService.toggle(anyLong(), anyLong(), anyString(), any())).thenReturn("like");

            mockMvc.perform(post("/api/feedback/100").param("type", "like"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.feedback").value("like"));
        }

        @Test @DisplayName("查询反馈统计返回计数")
        void shouldGetStats() throws Exception {
            FeedbackStatsDTO dto = new FeedbackStatsDTO();
            dto.setMessageId(100L); dto.setLikeCount(5L); dto.setDislikeCount(2L);
            when(feedbackService.getStats(eq(100L), any())).thenReturn(dto);

            mockMvc.perform(get("/api/feedback/stats/100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.likeCount").value(5));
        }
    }

    @Nested @DisplayName("个人中心 API - GET /api/my")
    class MyApi {
        @Test @DisplayName("返回点赞列表")
        void shouldListLikes() throws Exception {
            MyItemVO item = new MyItemVO();
            item.setId(1L); item.setType("course"); item.setTargetId(1L);
            item.setTitle("Java入门"); item.setLikeCount(10);
            item.setCreatedAt(LocalDateTime.now());
            when(myService.listLikes(anyLong(), anyInt(), anyInt())).thenReturn(List.of(item));

            mockMvc.perform(get("/api/my/likes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].title").value("Java入门"));
        }

        @Test @DisplayName("返回收藏列表")
        void shouldListFavorites() throws Exception {
            when(myService.listFavorites(anyLong(), anyInt(), anyInt())).thenReturn(List.of());

            mockMvc.perform(get("/api/my/favorites"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }
    }
}
