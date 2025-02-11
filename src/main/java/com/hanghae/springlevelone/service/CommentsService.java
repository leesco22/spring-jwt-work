package com.hanghae.springlevelone.service;

import com.hanghae.springlevelone.dto.CommentsRequestDto;
import com.hanghae.springlevelone.dto.CommentsResponseDto;
import com.hanghae.springlevelone.entity.Comment;
import com.hanghae.springlevelone.entity.Post;
import com.hanghae.springlevelone.jwt.JwtUtil;
import com.hanghae.springlevelone.repository.CommentsRepository;
import com.hanghae.springlevelone.repository.PostRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class CommentsService {
    private final CommentsRepository commentsRepository;
    private final PostRepository postRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public ResponseEntity<Object> createComment(CommentsRequestDto commentsRequestDto, Long id, HttpServletRequest request )   {
        Claims claims = getClaimsFromToken(request);
        if (claims == null) {
            return ResponseEntity.badRequest().body("토큰이 유효하지 않습니다.");
        }

        Post post = postRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 게시글 입니다.")
        );

        Comment comment = commentsRepository.saveAndFlush(new Comment(commentsRequestDto, claims.getSubject()));
        comment.setPost(post);

        return ResponseEntity.ok().body(new CommentsResponseDto(comment));
    }

    @Transactional
    public ResponseEntity<Object> updateComment(CommentsRequestDto commentsRequestDto, Long id, HttpServletRequest request) {
        Claims claims = getClaimsFromToken(request);
        if (claims == null) {
            return ResponseEntity.badRequest().body("토큰이 유효하지 않습니다.");
        }

        Comment comment = commentsRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 댓글 입니다.")
        );

        if (!userCheck(comment, claims)) {
            return ResponseEntity.badRequest().body("수정 / 삭제 권한이 없습니다.");
        }
        comment.update(commentsRequestDto);

        return ResponseEntity.ok().body(new CommentsResponseDto(comment));
    }

    @Transactional
    public ResponseEntity<Object> deleteComment(Long id, HttpServletRequest request ) {
        Claims claims = getClaimsFromToken(request);
        if (claims == null) {
            return ResponseEntity.badRequest().body("토큰이 유효하지 않습니다.");
        }

        Comment comment = commentsRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 댓글 입니다.")
        );

        if (!userCheck(comment, claims)) {
            return ResponseEntity.badRequest().body("수정 / 삭제 권한이 없습니다.");
        }
        commentsRepository.delete(comment);

        return ResponseEntity.ok().body("댓글이 삭제 됐습니다.");
    }

    private Claims getClaimsFromToken(HttpServletRequest request) {
        String token = jwtUtil.resolveToken(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            return null;
        }
        return jwtUtil.getInfoFromToken(token);
    }

    public boolean userCheck(Comment comment, Claims claims) {
        if (!comment.getUsername().equals(claims.getSubject())) {
            if (claims.get("auth").equals("ADMIN")) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }
}


