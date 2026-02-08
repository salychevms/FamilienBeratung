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
        if (!EMAIL_PATTERN.matcher(email).matches()) fail("Invalid email address");
    }

    public void validateIp(String ip) {
        if (ip == null || ip.isBlank()) return;
        InetAddressValidator v = InetAddressValidator.getInstance();
        if (!v.isValidInet4Address(ip) && !v.isValidInet6Address(ip)) fail("Invalid ip address");
    }

    public void validatePasswordCreate(String password, String login,
                                       String email, String mobileNumber, String landNumber) {
        if (password == null || password.isBlank()) fail("Password cannot be empty");
        if (password.length() < 12) fail("Password has to be at least 12 characters");
        if (!password.matches(".*[A-Z].*")) fail("Password has to contain uppercase characters");
        if (!password.matches(".*[a-z].*")) fail("Password has to contain lowercase characters");
        if (!password.matches(".*\\d.*")) fail("Password has to contain digits");
        if (!password.matches(".*[^A-Za-z0-9].*")) fail("Password has to contain special characters");
        if (!password.equals(password.trim())) fail("Password must not start or end with spaces");
        if (login == null || login.isBlank()) fail("Login is required for password validation");
        else if (password.equalsIgnoreCase(login)) fail("Password and Login cannot be the same");
        if (email != null && !email.isBlank())
            if (password.equalsIgnoreCase(email)) fail("Password and Email cannot be the same");
        if (mobileNumber != null && !mobileNumber.isBlank())
            if (password.equalsIgnoreCase(mobileNumber)) fail("Password and Mobile Number cannot be the same");
        if (landNumber != null && !landNumber.isBlank())
            if (password.equalsIgnoreCase(landNumber)) fail("Password and Land Number cannot be the same");
    }

    public void validatePasswordChange(String newPassword, String login, String email, String mobileNumber,
                                       String landNumber) {
        if (newPassword == null || newPassword.isBlank()) fail("Password cannot be empty");
        if (newPassword.length() < 12) fail("Password has to be at least 12 characters");
        if (!newPassword.matches(".*[A-Z].*")) fail("Password has to contain uppercase characters");
        if (!newPassword.matches(".*[a-z].*")) fail("Password has to contain lowercase characters");
        if (!newPassword.matches(".*\\d.*")) fail("Password has to contain digits");
        if (!newPassword.matches(".*[^A-Za-z0-9].*")) fail("Password has to contain special characters");
        if (!newPassword.equals(newPassword.trim())) fail("Password must not start or end with spaces");
        if (login == null || login.isBlank()) fail("Login is required for password validation");
        else if (newPassword.equalsIgnoreCase(login)) fail("Password and Login cannot be the same");
        if (email != null && !email.isBlank())
            if (newPassword.equalsIgnoreCase(email))
                fail("Password and Email cannot be the same");
        if (mobileNumber != null && !mobileNumber.isBlank())
            if (newPassword.equalsIgnoreCase(mobileNumber))
                fail("Password and Mobile Number cannot be the same");
        if (landNumber != null && !landNumber.isBlank())
            if (newPassword.equalsIgnoreCase(landNumber))
                fail("Password and Land Number cannot be the same");
    }

    public void validateBirthday(LocalDate birthday) {
        if (birthday == null) fail("Birth date is null");
        if (birthday.isAfter(LocalDate.now())) fail("Birth date is future");
        if (birthday.isBefore(LocalDate.of(1935, 1, 1))) fail("Birth date is too old");
    }


    public void validateText(String text, int maxLength) {
        if (text == null || text.isBlank()) return;
        String v = text.trim();
        if (v.length() > maxLength) fail("Text is too long (" + text.length() + "), max length is: " + maxLength);
        if (!v.chars().allMatch(c -> c >= 32 || c == 10 || c == 13)) fail("Invalid text characters");
    }

    private void fail(String message) {
        log.warn("Validation failed: " + message);
        throw new RuntimeException(message);
    }
}