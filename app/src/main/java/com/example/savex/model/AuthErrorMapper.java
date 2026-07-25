package com.example.savex.model;

import com.amazonaws.services.cognitoidentityprovider.model.InvalidParameterException;
import com.amazonaws.services.cognitoidentityprovider.model.InvalidPasswordException;
import com.amazonaws.services.cognitoidentityprovider.model.NotAuthorizedException;
import com.amazonaws.services.cognitoidentityprovider.model.UserNotConfirmedException;
import com.amazonaws.services.cognitoidentityprovider.model.UserNotFoundException;
import com.amazonaws.services.cognitoidentityprovider.model.UsernameExistsException;

/** Converts AWS Cognito's technical exceptions into short, user-friendly messages. */
public class AuthErrorMapper {

    public static String toUserMessage(Exception e) {
        if (e instanceof UsernameExistsException) {
            return "An account with this email already exists. Try signing in instead.";
        }
        if (e instanceof InvalidPasswordException) {
            return "Password must be at least 8 characters and include a number and a symbol.";
        }
        if (e instanceof NotAuthorizedException) {
            return "Incorrect email or password. Please try again.";
        }
        if (e instanceof UserNotFoundException) {
            return "No account found with this email.";
        }
        if (e instanceof UserNotConfirmedException) {
            return "This account hasn't been verified yet. Please check your email for the code.";
        }
        if (e instanceof InvalidParameterException) {
            return "Please check the details you entered and try again.";
        }
        // Fallback for anything unexpected (e.g. no internet connection)
        return "Something went wrong. Please check your connection and try again.";
    }
}