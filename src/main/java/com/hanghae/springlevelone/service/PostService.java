package com.hanghae.springlevelone.service;

import com.hanghae.springlevelone.dto.CommentsResponseDto;
import com.hanghae.springlevelone.dto.PostRequestDto;
import com.hanghae.springlevelone.dto.PostResponseDto;
import com.hanghae.springlevelone.entity.Comment;
import com.hanghae.springlevelone.entity.Post;
import com.hanghae.springlevelone.entity.User;
import com.hanghae.springlevelone.jwt.JwtUtil;
import com.hanghae.springlevelone.message.Message;
import com.hanghae.springlevelone.repository.CommentsRepository;
import com.hanghae.springlevelone.repository.PostRepository;
import com.hanghae.springlevelone.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentsRepository commentsRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public PostResponseDto createPost(PostRequestDto postRequestDto, HttpServletRequest request) {
        String token = jwtUtil.resolveToken(request);
        Claims claims;

        if (token != null) {
            if (jwtUtil.validateToken(token)) {
                claims = jwtUtil.getInfoFromToken(token);
            } else {
                throw new IllegalArgumentException("Token Error");
            }
            User user = userRepository.findByUsername(claims.getSubject()).orElseThrow(
                    () -> new IllegalArgumentException("존재하지 않는 아이디 입니다.")
            );

            Post post = postRepository.saveAndFlush(new Post(postRequestDto, user));
            post.setUser(user);

            return new PostResponseDto(post);
        } else {
            throw new NullPointerException("토큰이 존재하지 않습니다.");
        }
    }

    @Transactional(readOnly = true)
    public List<PostResponseDto> getPostList() {
        List<Post> posts = postRepository.findAllByOrderByCreatedAtDesc();
        List<PostResponseDto> postResponseDtos = new ArrayList<>();
        for (Post post : posts) {
            List<Comment> comments = commentsRepository.findAllByPostIdOrderByCreatedAtDesc(post.getId());
            List<CommentsResponseDto> commentsResponseDtoList = comments.stream().map(CommentsResponseDto::new).collect(Collectors.toList());
            PostResponseDto postResponseDto = new PostResponseDto(post);
            postResponseDto.setComments(commentsResponseDtoList);
            postResponseDtos.add(postResponseDto);
        }
        return postResponseDtos;
    }

    public PostResponseDto getPost(Long id) {
        Post post = postRepository.findById(id).orElseThrow(
                () -> new NullPointerException("해당 게시글이 존재하지 않습니다.")
        );
        List<Comment> comments = commentsRepository.findAllByPostIdOrderByCreatedAtDesc(id);
        List<CommentsResponseDto> commentsResponseDtoList = comments.stream().map(CommentsResponseDto::new).collect(Collectors.toList());
        PostResponseDto postResponseDto = new PostResponseDto(post);
        postResponseDto.setComments(commentsResponseDtoList);
        return postResponseDto;
    }

    @Transactional
    public PostResponseDto updatePost(Long id, PostRequestDto postRequestDto, HttpServletRequest request) {

        String token = jwtUtil.resolveToken(request);
        Claims claims;

        if (token != null) {
            if (jwtUtil.validateToken(token)) {
                claims = jwtUtil.getInfoFromToken(token);
            } else {
                throw new IllegalArgumentException("Token Error");
            }

            Post post = checkPost(id);
            if (post.getUsername().equals(claims.getSubject())) {
                post.update(postRequestDto);
            } else {
                throw new IllegalArgumentException("작성자가 아니면 수정할 수 없습니다.");
            }
            List<Comment> comments = commentsRepository.findAllByPostIdOrderByCreatedAtDesc(id);
            List<CommentsResponseDto> commentsResponseDtoList = comments.stream().map(CommentsResponseDto::new).collect(Collectors.toList());
            PostResponseDto postResponseDto = new PostResponseDto(post);
            postResponseDto.setComments(commentsResponseDtoList);
            return  postResponseDto;

        } else {
            throw new NullPointerException("토큰이 존재하지 않습니다.");
        }
    }

    public ResponseEntity<Message> deletePost(Long id, HttpServletRequest request) {
        String token = jwtUtil.resolveToken(request);
        Claims claims;

        if (token != null) {
            if (jwtUtil.validateToken(token)) {
                claims = jwtUtil.getInfoFromToken(token);
            } else {
                throw new IllegalArgumentException("Token Error");
            }

            Post post = checkPost(id);
            if (post.getUsername().equals(claims.getSubject())) {
                postRepository.delete(post);
            } else {
                throw new IllegalArgumentException("작성자가 아니면 삭제할 수 없습니다.");
            }

            Message message = new Message();
            message.setMsg("게시글이 삭제됐습니다.");
            message.setStatusCode(HttpStatus.OK.value());
            return ResponseEntity.ok(message);
        } else {
            throw new NullPointerException("토큰이 존재하지 않습니다.");
        }
    }

    //method 분리-------------------------------------------------------------------------------------
    public Post checkPost(Long id){
        return postRepository.findById(id).orElseThrow(
                () -> new NullPointerException("해당 게시글이 존재하지 않습니다.")
        );
    }

    public Claims tokenCheck(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String token = jwtUtil.resolveToken(request);
        Claims claims;

        if (token == null || !jwtUtil.validateToken(token)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "토큰이 유효하지 않습니다.");
            return null;
        }

        claims = jwtUtil.getInfoFromToken(token);
        return claims;
    }

    public boolean userCheck(Comment comment, Claims claims, HttpServletResponse response) throws IOException {
        if (!comment.getUsername().equals(claims.getSubject())) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "작성자만 수정/삭제할 수 있습니다.");
            return false;
        }
        return true;
    }
}
