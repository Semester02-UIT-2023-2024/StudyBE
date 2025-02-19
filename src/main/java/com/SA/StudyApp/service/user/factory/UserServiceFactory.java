package com.SA.StudyApp.service.user.factory;

import com.SA.StudyApp.constant.APIStatus;
import com.SA.StudyApp.dto.request.user.UserRequest;
import com.SA.StudyApp.dto.request.user.UserUpdateRequest;
import com.SA.StudyApp.exception.BusinessException;
import com.SA.StudyApp.model.Image;
import com.SA.StudyApp.model.user.Role;
import com.SA.StudyApp.model.user.Status;
import com.SA.StudyApp.model.user.User;
import com.SA.StudyApp.repository.image.ImageRepository;
import com.SA.StudyApp.repository.user.UserRepository;
import com.SA.StudyApp.service.user.service.UserDetailService;
import com.SA.StudyApp.util.fileUpload.FileUploadImp;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@RequiredArgsConstructor
public abstract class UserServiceFactory  {
    private final PasswordEncoder passwordEncoder;
    private final ImageRepository imageRepository;
    private final UserDetailService userDetailService;
    private final UserRepository userRepository;
    private final FileUploadImp fileUploadImp;
    protected abstract User createUser(User user, UserRequest userRequest);

    protected abstract User updateUser(User user, UserUpdateRequest userRequest);
    protected abstract List<User> getAllUsersByRole(Integer role);

    @Transactional
    public User create(UserRequest userRequest, Role role, MultipartFile image) throws IOException {
        userRepository.findByEmail(userRequest.getEmail()).ifPresent(user -> {
            throw new BusinessException(APIStatus.EMAIL_ALREADY_EXISTED);
        });
        userRepository.findByPhone(userRequest.getPhone()).ifPresent(user -> {
            throw new BusinessException(APIStatus.PHONE_ALREADY_EXISTED);
        });
        User user = User.builder()
                .fullName(userRequest.getFullName())
                .email(userRequest.getEmail())
                .phone(userRequest.getPhone())
                .password(passwordEncoder.encode(userRequest.getPassword()))
                .enabled(userRequest.getEnable() == Status.ACTIVE.ordinal())
                .role(role)
                .build();
        user.setCommonCreate(userDetailService.getIdLogin());

        var userImage = image;
        if(userImage != null){
            BufferedImage bi = ImageIO.read(userImage.getInputStream());
            if (bi == null) {
                throw new BusinessException(APIStatus.IMAGE_NOT_FOUND);
            }
            Map result = fileUploadImp.upload(userImage, "avatars");
            Image avatar =  Image.builder()
                    .name((String) result.get("original_filename"))
                    .url((String) result.get("url"))
                    .cloudinaryId((String) result.get("public_id"))
                    .build();
            imageRepository.save(avatar);
            user.setImage(avatar);
        }
        return createUser(user, userRequest);
    }

    public User update(UUID userId, UserUpdateRequest userRequest, MultipartFile image) throws IOException {
        User user = userRepository.findById(userId).orElseThrow(() ->{
                    throw new BusinessException(APIStatus.USER_NOT_FOUND);
                }
        );
        String oldPhone = user.getPhone();
        String newPhone = userRequest.getPhone();
        userRepository.findByPhone(newPhone).ifPresent(u -> {
            if(!oldPhone.equals(newPhone)) {
                throw new BusinessException(APIStatus.PHONE_ALREADY_EXISTED);
            }
        });
        user.setFullName(userRequest.getFullName());
        user.setPhone(userRequest.getPhone());

        var userImage = image;
        if(userImage != null){
            if(user.getImage() != null){
                fileUploadImp.delete(user.getImage().getCloudinaryId());
            }
            BufferedImage bi = ImageIO.read(userImage.getInputStream());
            if (bi == null) {
                throw new BusinessException(APIStatus.IMAGE_NOT_FOUND);
            }
            Map result = fileUploadImp.upload(userImage, "avatars");
            Image avatar =  Image.builder()
                    .name((String) result.get("original_filename"))
                    .url((String) result.get("url"))
                    .cloudinaryId((String) result.get("public_id"))
                    .build();
            imageRepository.save(avatar);
            user.setImage(avatar);
        }
        user.setCommonUpdate(userDetailService.getIdLogin());

        return updateUser(user, userRequest);
    }

    public List<User> getAllUsers(Integer role){
        return userRepository.findByRole(Role.convertIntegerToRole(role))
                .orElse(null);
    }
}
