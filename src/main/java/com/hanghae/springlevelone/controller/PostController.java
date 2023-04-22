package com.hanghae.springlevelone.controller;

import com.hanghae.springlevelone.dto.PostRequestDto;
import com.hanghae.springlevelone.dto.PostResponseDto;
import com.hanghae.springlevelone.service.PostService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/blog")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping("/posting")
    public PostResponseDto createPost(@RequestBody PostRequestDto postRequestDto, HttpServletRequest request) {
        return postService.createPost(postRequestDto, request);
    }
    @GetMapping("/list")
    public List<PostResponseDto> getPostList() {
        return postService.getPostList();
    }
    @GetMapping("/{id}")
    public PostResponseDto getPost(@PathVariable Long id){
        return postService.getPost(id);
    }
    @PutMapping("/{id}")
    public PostResponseDto updatePost(@PathVariable Long id, @RequestBody PostRequestDto postRequestDto, @RequestParam(name = "password") String password) {
        return postService.updatePost(id, postRequestDto, password);
    }
    @DeleteMapping("/{id}")
    public String deletePost(@PathVariable Long id,@RequestParam(name = "password") String password) {
        return postService.deletePost(id, password);
    }
}
