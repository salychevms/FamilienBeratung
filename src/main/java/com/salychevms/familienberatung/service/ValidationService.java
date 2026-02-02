package com.salychevms.familienberatung.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.apache.commons.validator.routines.InetAddressValidator;

import java.time.LocalDate;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ValidationService {
    private final static Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private final static double SIMILARITY_LIMIT = 0.8;

    public void validateEmail(String email) {
        if (email == null || email.isEmpty()) return;
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            log.error("Invalid email address");
            throw new RuntimeException("Invalid email address");
        }
    }

    public void validateIp(String ip) {
        if (ip == null || ip.isBlank()) return;
        InetAddressValidator v = InetAddressValidator.getInstance();
        if (!v.isValidInet4Address(ip) && !v.isValidInet6Address(ip)) {
            log.error("Invalid ip address");
            throw new RuntimeException("Invalid ip address");
        }
    }

    public void validatePassword(String password, String oldPassword, String login,
                                 String email, String mobileNumber, String landNumber) {
        if (password == null || password.isBlank()) {
            log.error("Password is empty");
            throw new RuntimeException("Password cannot be empty");
        }
        if (password.length() < 12) {
            log.error("Password has to be at least 12 characters");
            throw new RuntimeException("Password has to be at least 12 characters");
        }
        if (!password.matches(".*[A-Z].*")) {
            log.error("Password has to contain uppercase characters");
            throw new RuntimeException("Password has to contain uppercase characters");
        }
        if (!password.matches(".*[a-z].*")) {
            log.error("Password has to contain lowercase characters");
            throw new RuntimeException("Password has to contain lowercase characters");
        }
        if (!password.matches(".*\\d.*")) {
            log.error("Password has to contain digits");
            throw new RuntimeException("Password has to contain digits");
        }
        if (!password.matches(".*[^A-Za-z0-9].*")) {
            log.error("Password has to contain special characters");
            throw new RuntimeException("Password has to contain special characters");
        }
        if (password.trim().length() != password.length()) {
            log.error("Password hasn't start or end with no spaces");
            throw new RuntimeException("Password hasn't start or end with no spaces");
        }
        if (login != null) {
            if (password.equalsIgnoreCase(login)) {
                log.error("Password and Login cannot be the same");
                throw new RuntimeException("Password and Login cannot be the same");
            }
        }else {
            log.warn("Login is null");
            throw new RuntimeException("Login is null");
        }
        if (email != null) {
            if (password.equalsIgnoreCase(email)) {
                log.error("Password and Email cannot be the same");
                throw new RuntimeException("Password and Email cannot be the same");
            }
        }else {
            log.warn("Email is null");
            throw new RuntimeException("Email is null");
        }
        if (mobileNumber != null) {
            if (password.equalsIgnoreCase(mobileNumber)) {
                log.error("Password and Mobile Number cannot be the same");
                throw new RuntimeException("Password and Mobile Number cannot be the same");
            }
        }else {
            log.warn("Mobile Number is null");
            throw new RuntimeException("Mobile Number is null");
        }
        if (landNumber != null) {
            if (password.equalsIgnoreCase(landNumber)) {
                log.error("Password and Land cannot be the same");
                throw new RuntimeException("Password and Land cannot be the same");
            }
        }else{
            log.warn("Land Number is null");
            throw new RuntimeException("Land Number is null");
        }
        if (oldPassword != null && similarity(password, oldPassword) > SIMILARITY_LIMIT) {
            log.error("New Password and Old Password cannot be too similar");
            throw new RuntimeException("New Password and Old Password cannot be too similar");
        }
    }

    private double similarity(String a, String b) {
        int max = Math.max(a.length(), b.length());
        int same = 0;
        for (int i = 0; i < Math.min(a.length(), b.length()); i++) {
            if (a.charAt(i) == b.charAt(i)) same++;
        }
        return (double) same / (double) max;
    }

    public void validateBirthday(LocalDate birthday) {
        if (birthday == null) {
            log.error("Birth date is null");
            throw new RuntimeException("Birth date is null");
        }
        if (birthday.isAfter(LocalDate.now())) {
            log.error("Birth date is future");
            throw new RuntimeException("Birth date is future");
        }
        if (birthday.isBefore(LocalDate.of(1935, 1, 1))) {
            log.error("Birth date is too old");
            throw new RuntimeException("Birth date is too old");
        }
    }

    public void validateFile(String originalName, long size, String type) {
        if (size <= 0) {
            log.error("File size is zero");
            throw new RuntimeException("File size is zero");
        }
        if (size > 10_000_000) {
            log.error("File size is more than 10 MB");
            throw new RuntimeException("File size is more than 10 MB");
        }
        if (originalName == null || originalName.contains("..") || originalName.contains("/") || originalName.contains("\\")) {
            log.error("Invalid file name");
            throw new RuntimeException("Invalid file name");
        }
        if (!type.matches("(?i)pdf|jpg|jpeg|png|docx")) {
            log.error("Invalid file type");
            throw new RuntimeException("Invalid file type");
        }
    }

    public void validateText(String text, int maxLength) {
        if (text == null || text.isBlank()) return;
        String v = text.trim();
        if (v.length() > maxLength) {
            log.error("Text is too long ({}), max length is {}", text.length(), maxLength);
            throw new RuntimeException("Text is too long (" + text.length() + "), max length is: " + maxLength);
        }
        if (!v.chars().allMatch(c -> c >= 32 || c == 10 || c == 13)) {
            log.error("Invalid text characters");
            throw new RuntimeException("Invalid text characters");
        }
    }
}