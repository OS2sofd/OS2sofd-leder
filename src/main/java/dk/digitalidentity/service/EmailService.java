package dk.digitalidentity.service;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import dk.digitalidentity.config.settings.ManagerConfiguration;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailService {

	@Autowired
	private ManagerConfiguration configuration;

	public boolean sendMessage(String email, String subject, String message, String replyTo) {
		if (!isConfigured()) {
			log.warn("email server is not configured - not sending email to " + email);
			return false;
		}

		Transport transport = null;
		log.info("Sending email: '" + subject + "' to " + email);

		try {
			Properties props = System.getProperties();
			props.put("mail.transport.protocol", "smtps");
			props.put("mail.smtp.port", 465);
			props.put("mail.smtp.auth", "true");
			props.put("mail.smtp.starttls.enable", "true");
			props.put("mail.smtp.starttls.required", "true");
			Session session = Session.getDefaultInstance(props);

			MimeMessage msg = new MimeMessage(session);

			if (StringUtils.hasLength(replyTo)) {
				msg.setReplyTo(new Address[] {
					new InternetAddress(replyTo)
				});
			}

			msg.setFrom(new InternetAddress(
				configuration.getEmail().getFrom(),
				configuration.getEmail().getFromName()
			));
			msg.setRecipient(Message.RecipientType.TO, new InternetAddress(email));
			msg.setSubject(subject, "UTF-8");
			msg.setText(message, "UTF-8");
			msg.setHeader("Content-Type", "text/html; charset=UTF-8");

			transport = session.getTransport();
			transport.connect(
				configuration.getEmail().getHost(),
				configuration.getEmail().getUsername(),
				configuration.getEmail().getPassword()
			);
			transport.addTransportListener(new TransportErrorHandler());
			transport.sendMessage(msg, msg.getAllRecipients());
		}
		catch (Exception ex) {
			log.error("Failed to send email to '" + email + "'", ex);

			return false;
		}
		finally {
			try {
				if (transport != null) {
					transport.close();
				}
			}
			catch (Exception ex) {
				log.warn("Error occured while trying to terminate connection", ex);
			}
		}

		return true;
	}


	private boolean isConfigured() {
		if (!configuration.getEmail().isEnabled() || configuration.getEmail().getHost() == null || configuration.getEmail().getHost().length() == 0 || configuration.getEmail().getPassword()  == null || configuration.getEmail().getPassword().length() == 0 || configuration.getEmail().getUsername() == null || configuration.getEmail().getUsername().length() == 0) {
			return false;
		}

		return true;
	}
}
