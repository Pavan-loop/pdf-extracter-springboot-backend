package com.madara.security.service.impl;

import com.madara.security.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "PDFExtractor");
            helper.setTo(toEmail);
            helper.setSubject("Your PDFExtractor Verification Code");
            helper.setText(buildHtml(otpCode), true);

            mailSender.send(message);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send verification email. Please try again.");
        }
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String otpCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "PDFExtractor");
            helper.setTo(toEmail);
            helper.setSubject("PDFExtractor – Password Reset Request");
            helper.setText(buildPasswordResetHtml(otpCode), true);

            mailSender.send(message);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send password reset email. Please try again.");
        }
    }

    private String buildHtml(String otpCode) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
              <title>Verify your email</title>
            </head>
            <body style="margin:0;padding:0;background-color:#f4f6f9;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%" cellpadding="0" cellspacing="0" style="background-color:#f4f6f9;padding:40px 0;">
                <tr>
                  <td align="center">
                    <table width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%;">

                      <!-- Header -->
                      <tr>
                        <td align="center" style="background:linear-gradient(135deg,#1a73e8,#0d47a1);border-radius:12px 12px 0 0;padding:36px 40px;">
                          <h1 style="margin:0;color:#ffffff;font-size:28px;font-weight:700;letter-spacing:-0.5px;">
                            &#128196; PDFExtractor
                          </h1>
                          <p style="margin:8px 0 0;color:#c2d9ff;font-size:14px;">Intelligent Document Processing</p>
                        </td>
                      </tr>

                      <!-- Body -->
                      <tr>
                        <td style="background:#ffffff;padding:40px 48px;">
                          <h2 style="margin:0 0 8px;color:#1a1a2e;font-size:22px;font-weight:600;">Verify your email address</h2>
                          <p style="margin:0 0 28px;color:#5f6368;font-size:15px;line-height:1.6;">
                            Thanks for signing up! Use the verification code below to complete your registration.
                            This code is valid for <strong>10 minutes</strong>.
                          </p>

                          <!-- OTP Box -->
                          <table width="100%" cellpadding="0" cellspacing="0">
                            <tr>
                              <td align="center" style="padding:8px 0 32px;">
                                <div style="display:inline-block;background:#f0f5ff;border:2px dashed #1a73e8;border-radius:12px;padding:24px 48px;">
                                  <p style="margin:0 0 4px;color:#5f6368;font-size:12px;text-transform:uppercase;letter-spacing:1.5px;font-weight:600;">Your OTP Code</p>
                                  <p style="margin:0;color:#1a73e8;font-size:42px;font-weight:700;letter-spacing:10px;font-family:'Courier New',monospace;">""" + otpCode + """
                                  </p>
                                </div>
                              </td>
                            </tr>
                          </table>

                          <!-- Warning -->
                          <table width="100%" cellpadding="0" cellspacing="0" style="margin-bottom:28px;">
                            <tr>
                              <td style="background:#fff8e1;border-left:4px solid #f9a825;border-radius:4px;padding:14px 18px;">
                                <p style="margin:0;color:#5f4b00;font-size:13px;line-height:1.5;">
                                  &#x26A0;&#xFE0F;&nbsp; <strong>Never share this code with anyone.</strong>
                                  PDFExtractor will never ask for your OTP via phone or chat.
                                </p>
                              </td>
                            </tr>
                          </table>

                          <p style="margin:0;color:#9aa0a6;font-size:13px;line-height:1.6;">
                            If you didn't create a PDFExtractor account, you can safely ignore this email.
                            Someone may have entered your email address by mistake.
                          </p>
                        </td>
                      </tr>

                      <!-- Footer -->
                      <tr>
                        <td style="background:#f8f9fa;border-radius:0 0 12px 12px;padding:24px 48px;border-top:1px solid #e8eaed;">
                          <p style="margin:0;color:#9aa0a6;font-size:12px;text-align:center;line-height:1.6;">
                            &copy; 2025 PDFExtractor &bull; This is an automated message, please do not reply.
                          </p>
                        </td>
                      </tr>

                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """;
    }

    private String buildPasswordResetHtml(String otpCode) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
              <title>Reset your password</title>
            </head>
            <body style="margin:0;padding:0;background-color:#f4f6f9;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%" cellpadding="0" cellspacing="0" style="background-color:#f4f6f9;padding:40px 0;">
                <tr>
                  <td align="center">
                    <table width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%;">

                      <!-- Header -->
                      <tr>
                        <td align="center" style="background:linear-gradient(135deg,#e53935,#b71c1c);border-radius:12px 12px 0 0;padding:36px 40px;">
                          <h1 style="margin:0;color:#ffffff;font-size:28px;font-weight:700;letter-spacing:-0.5px;">
                            &#128196; PDFExtractor
                          </h1>
                          <p style="margin:8px 0 0;color:#ffcdd2;font-size:14px;">Intelligent Document Processing</p>
                        </td>
                      </tr>

                      <!-- Body -->
                      <tr>
                        <td style="background:#ffffff;padding:40px 48px;">
                          <h2 style="margin:0 0 8px;color:#1a1a2e;font-size:22px;font-weight:600;">Password Reset Request</h2>
                          <p style="margin:0 0 28px;color:#5f6368;font-size:15px;line-height:1.6;">
                            We received a request to reset your PDFExtractor password. Use the code below to proceed.
                            This code is valid for <strong>10 minutes</strong>.
                          </p>

                          <!-- OTP Box -->
                          <table width="100%" cellpadding="0" cellspacing="0">
                            <tr>
                              <td align="center" style="padding:8px 0 32px;">
                                <div style="display:inline-block;background:#fff5f5;border:2px dashed #e53935;border-radius:12px;padding:24px 48px;">
                                  <p style="margin:0 0 4px;color:#5f6368;font-size:12px;text-transform:uppercase;letter-spacing:1.5px;font-weight:600;">Reset Code</p>
                                  <p style="margin:0;color:#e53935;font-size:42px;font-weight:700;letter-spacing:10px;font-family:'Courier New',monospace;">""" + otpCode + """
                                  </p>
                                </div>
                              </td>
                            </tr>
                          </table>

                          <!-- Warning -->
                          <table width="100%" cellpadding="0" cellspacing="0" style="margin-bottom:28px;">
                            <tr>
                              <td style="background:#fff8e1;border-left:4px solid #f9a825;border-radius:4px;padding:14px 18px;">
                                <p style="margin:0;color:#5f4b00;font-size:13px;line-height:1.5;">
                                  &#x26A0;&#xFE0F;&nbsp; <strong>Never share this code with anyone.</strong>
                                  PDFExtractor support will never ask for this code.
                                </p>
                              </td>
                            </tr>
                          </table>

                          <p style="margin:0;color:#9aa0a6;font-size:13px;line-height:1.6;">
                            If you did not request a password reset, please ignore this email.
                            Your password will remain unchanged.
                          </p>
                        </td>
                      </tr>

                      <!-- Footer -->
                      <tr>
                        <td style="background:#f8f9fa;border-radius:0 0 12px 12px;padding:24px 48px;border-top:1px solid #e8eaed;">
                          <p style="margin:0;color:#9aa0a6;font-size:12px;text-align:center;line-height:1.6;">
                            &copy; 2025 PDFExtractor &bull; This is an automated message, please do not reply.
                          </p>
                        </td>
                      </tr>

                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """;
    }
}

