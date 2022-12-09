package com.example.speedsideproject.post;


import com.example.speedsideproject.account.entity.Account;
import com.example.speedsideproject.aws_s3.S3UploadUtil;
import com.example.speedsideproject.error.CustomException;
import com.example.speedsideproject.post.enums.Tech;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
public class PostService {
    private final PostRepository postRepository;
    private final S3UploadUtil s3UploadUtil;
    private final ImageRepository imageRepository;
    private final TechsRepository techsRepository;

    @Autowired
    public PostService(PostRepository postRepository, ImageRepository imageRepository, S3UploadUtil s3UploadUtil, TechsRepository techsRepository) {
        this.postRepository = postRepository;
        this.imageRepository = imageRepository;
        this.s3UploadUtil = s3UploadUtil;
        this.techsRepository = techsRepository;
    }

    // 모든 글 읽어오기
//    public List<PostResponseDto> getAllpost() {
//        return postRepository.findAllByOrderByCreatedAtDesc().stream().map(PostResponseDto::new).collect(Collectors.toList());
//    }
    @Transactional(readOnly=true)
    public Page<PostResponseDto> getAllPost(){

        return PostQueryRepository.findAllMyPostWithQuery();
    }

    //글쓰기
    @Transactional
    public PostResponseDto createPost(PostRequestDto postRequestDto, List<MultipartFile> imgFiles, List<Tech> techList, Account account) throws IOException {
        List<MultipartFile> img = imgFiles;
        Post post = new Post(postRequestDto, account);
        List<Image> imageList = new ArrayList<>();
        for (MultipartFile image : imgFiles) {
            Image image1 = new Image(s3UploadUtil.upload(image, "side-post"));
            imageList.add(image1);
            post.addImg(image1);
        }
        imageRepository.saveAll(imageList);

        //techs 추가
        List<Techs> techsList = techList.stream().map(te->new Techs(te,post)).collect(Collectors.toList());
        techsRepository.saveAll(techsList);
        postRepository.save(post);
        return new PostResponseDto(post);
    }

    //글 수정
    @Transactional
    public PostResponseDto updatePost(PostRequestDto requestDto, List<MultipartFile> imgFiles, Long id, Account account) {
        Post post = postRepository.findById(id).orElseThrow(
                () -> new CustomException(CANNOT_FIND_POST_NOT_EXIST));
        if (!account.getEmail().equals(post.getAccount().getEmail())) {
            throw new CustomException(NOT_FOUND_USER);
        }

        post.update(requestDto);
        return new PostResponseDto(post);
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
        if (!(post.getUrlKey() == null)) {
            s3UploadUtil.delete(post.getUrlKey());
        }
        return "delete success";
    }

    //글 1개 get
    public PostResponseDto getOnePost(Account account) {
        Post post = postRepository.findById(account.getId()).orElseThrow(() -> new CustomException(CANNOT_FIND_POST_NOT_EXIST));
        return new PostResponseDto(post);
    }

}
