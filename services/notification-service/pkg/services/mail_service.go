package services

import (
	"net/smtp"

	log "github.com/sirupsen/logrus"
	"github.com/sunbirdrc/notification-service/config"
)

func SendEmail(recipientEmail string, mailSubject string, mailBody string) error {
	if config.Config.EmailSMTP.Enable {
		from := config.Config.EmailSMTP.FromAddress
		pass := config.Config.EmailSMTP.Password

		msg := "From: " + from + "\n" +
			"To: " + recipientEmail + "\n" +
			"Subject: " + mailSubject + "\n\n" +
			mailBody

		err := smtp.SendMail("smtp.gmail.com:465",
			smtp.PlainAuth("", from, pass, "smtp.gmail.com"),
			from, []string{recipientEmail}, []byte(msg))

		if err != nil {
			log.Errorf("smtp error: %s", err)
			return err
		}
		return nil
	}
	log.Infof("EMAIL notifier disabled")
	return nil
}
