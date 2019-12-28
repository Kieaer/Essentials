package essentials.special;

import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;

public class sendMail {
    String smtp;
    int port;
    String email;
    String sender;
    String password;
    String targetname;
    String targetmail;
    String subject;
    String text;

    sendMail(String smtp, int port, String email, String password, String sender, String targetname, String targetmail, String subject, String text){
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

    public void main(){
        Mailer mailer = MailerBuilder
                .withSMTPServer(smtp, port, email, password)
                .withTransportStrategy(TransportStrategy.SMTP_TLS)
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

        mailer.sendMail(mail);
    }
}