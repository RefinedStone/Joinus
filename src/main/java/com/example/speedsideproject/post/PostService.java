package com.example.speedsideproject.post;

import com.example.speedsideproject.account.entity.Account;
import com.example.speedsideproject.aws_s3.S3UploadUtil;
import com.example.speedsideproject.error.CustomException;
import com.example.speedsideproject.likes.Likes;
import com.example.speedsideproject.likes.LikesRepository;
import com.example.speedsideproject.post.enums.Tech;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.speedsideproject.error.ErrorCode.CANNOT_FIND_POST_NOT_EXIST;
import static com.example.speedsideproject.error.ErrorCode.NOT_FOUND_USER;

@Service
@Slf4j
public class PostService {
    private final PostRepository postRepository;
    private final S3UploadUtil s3UploadUtil;
    private final ImageRepository imageRepository;
    private final TechsRepository techsRepository;
//    private final PostQueryRepository postQueryRepository;
    private final LikesRepository likesRepository;

    @Autowired
    public PostService(PostRepository postRepository, ImageRepository imageRepository, S3UploadUtil s3UploadUtil, TechsRepository techsRepository,
                        LikesRepository likesRepository) {
        this.postRepository = postRepository;
        this.imageRepository = imageRepository;
        this.s3UploadUtil = s3UploadUtil;
        this.techsRepository = techsRepository;
//        this.postQueryRepository = postQueryRepository;
        this.likesRepository = likesRepository;
    }

    // 모든 글 읽어오기
    @Transactional(readOnly = true)
    public Page<?> getAllPost(Pageable pageable) {
        return postRepository.findAllMyPost(pageable);
    }
//    // 무한 스크롤 모든 글 읽어오기
//    @Transactional(readOnly = true)
//    public PostListResponseDto getPost(Pageable pageable, Account account) {
//        Page<Post> postList = postQueryRepository.findAllMyPostWithQuery(pageable);
//        List<Likes> likeList = likesRepository.findLikesByAccount(account);
//        List<PostResponseDto> postResponseDtos = new ArrayList<>();
//        for (Post post : postList) {
//            postResponseDtos.add(new PostResponseDto(post, isLikedPost(post, likeList)));
//        }
//        return new PostListResponseDto(postResponseDtos, postList.getTotalElements());
//    }

    private boolean isLikedPost(Post post, List<Likes> likeList) {
        for (Likes like : likeList) {
            if (like.getPost() != null && like.getPost().getId().equals(post.getId())) {
                return true;
            }
        }
        return false;
    }

    //글쓰기
    @Transactional
    public PostResponseDto createPost(PostRequestDto postRequestDto, List<MultipartFile> imgFiles, List<Tech> techList, Account account) throws IOException {
        Post post = new Post(postRequestDto, account);

        List<Image> imageList = new ArrayList<>();

        for (MultipartFile image : imgFiles) {
            Image image1 = new Image(s3UploadUtil.upload(image, "side-post"));
            imageList.add(image1);
            post.addImg(image1);
        }
        imageRepository.saveAll(imageList);

        //techs 추가
        List<Techs> techsList = techList.stream().map(te -> new Techs(te, post)).collect(Collectors.toList());
        techsRepository.saveAll(techsList);
        postRepository.save(post);
        List<Likes> likeList = likesRepository.findLikesByAccount(account);
        return new PostResponseDto(post, isLikedPost(post, likeList));
    }

    //글 수정
    @Transactional

    public PostResponseDto updatePost(PostRequestDto requestDto, List<MultipartFile> imgFiles, List<Tech> techList, Long id, Account account) throws IOException {

        Post post = postRepository.findByIdAndAccount(id, account);
        if (post == null) throw new CustomException(NOT_FOUND_USER);

        List<Image> imageList = imageRepository.findAllByPostId(post.getId());

        for (Image i : imageList) {
            s3UploadUtil.delete(i.getImgKey());
            imageRepository.delete(i);
        }

        List<Image> images = new ArrayList<>();

        if (imgFiles != null) {
            for (MultipartFile m : imgFiles) {
                Image i = imageRepository.save(new Image(s3UploadUtil.upload(m, "side-post")));
                images.add(i);
                post.addImg(i);
            }
        }

        //요거보고 이미지도 바꾸세여?
        List<Techs> techLists = techsRepository.findAllByPostId(post.getId());
        techsRepository.deleteAllInBatch(techLists);

        List<Techs> techsList = techList.stream().map(te -> new Techs(te, post)).collect(Collectors.toList());
        techsRepository.saveAll(techsList);
        post.update(requestDto);
        List<Likes> likeList = likesRepository.findLikesByAccount(account);
        return new PostResponseDto(post, isLikedPost(post, likeList));
    }

    //글 삭제
    @Transactional
    public String deletePost(Long id, Account account) {
        Post post = postRepository.findById(id).orElseThrow(
                () -> new CustomException(CANNOT_FIND_POST_NOT_EXIST)
        );
        if (!account.getEmail().equals(post.getAccount().getEmail())) {
            throw new CustomException(NOT_FOUND_USER);
        }
        postRepository.deleteById(id);

        // post image를 List 형태로 불러와서 삭제해야한다.
        // 추후 변경 해야 함.
//        if (!(post.getUrlKey() == null)) {
//            s3UploadUtil.delete(post.getUrlKey());
//        }
        return "delete success";
    }

    //글 1개 get
    public PostResponseDto getOnePost(Long id, Account account) {
        Post post = postRepository.findById(id).orElseThrow(() -> new CustomException(CANNOT_FIND_POST_NOT_EXIST));
        List<Likes> likeList = likesRepository.findLikesByAccount(account);
        return new PostResponseDto(post, isLikedPost(post, likeList));
    }
}
