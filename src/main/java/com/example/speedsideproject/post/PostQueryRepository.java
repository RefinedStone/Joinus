package com.example.speedsideproject.post;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.speedsideproject.post.QPost.post;


@Repository
public class PostQueryRepository {
    private final JPAQueryFactory queryFactory;

    @Autowired
    public PostQueryRepository(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    //findAllMyPostWithQuery
    public Page<PostResponseDto> findAllMyPostWithQuery(Pageable pageable) {
        QPost qPost = post;

        List<Post> posts = queryFactory
                .select(post)
                .from(post)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        List<PostResponseDto> collect = posts.stream().map(PostResponseDto::new).collect(Collectors.toList());

        Long count = queryFactory
                .select(post.count())
                .from(post)
                .fetchOne();

        return new PageImpl<>(collect, pageable, count);
    }
}
