package com.SA.StudyApp.repository.user;

import com.SA.StudyApp.model.user.ForgotPassword;
import com.SA.StudyApp.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface ForgotPasswordRepository extends JpaRepository<ForgotPassword, Long> {
    @Query("select fp from ForgotPassword fp where fp.otp = ?1 and fp.user = ?2")
    Optional<ForgotPassword> findByOtpAndUser(Integer otp, User user);
}