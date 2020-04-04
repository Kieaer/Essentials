package remake.external;

import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.AsyncResponse;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
import remake.internal.CrashReport;

public class Mail {
    String smtp;
    int port;
    String email;
    String sender;
    String password;
    String targetname;
    String targetmail;
    String subject;
    String text;

    boolean result;

    public Mail(String smtp, int port, String email, String password, String sender, String targetname, String targetmail, String subject, String text) {
        this.smtp = smtp;
        this.port = port;
        this.email = email;
        this.password = password;
        this.sender = sender;
        this.targetname = targetname;
        this.targetmail = targetmail;
        this.subject = subject;
        this.text = text;
    }

    public boolean send() {
        // http://www.simplejavamail.org/features.html#navigation
        Mailer mailer = MailerBuilder
                .withSMTPServer(smtp, port, email, password)
                .withTransportStrategy(TransportStrategy.SMTPS)
                .withSessionTimeout(10 * 1000)
                .withDebugLogging(true)
                .async()
                .buildMailer();

        Email mail = EmailBuilder.startingBlank()
                .from(sender, email)
                .to(targetname, targetmail)
                .withSubject(subject)
                .withPlainText(text)
                .buildEmail();

        AsyncResponse response = mailer.sendMail(mail, true);

        if (response != null) {
            response.onSuccess(() -> result = true);
            response.onException((e) -> {
                new CrashReport(e);
                result = false;
            });
        } else {
            result = false;
        }

        return result;
    }
}