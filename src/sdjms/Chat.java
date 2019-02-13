package sdjms;

import javax.jms.JMSException;
import javax.jms.TopicConnection;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicConnectionFactory;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.jms.MessageListener;
import javax.naming.InitialContext;
import java.util.Scanner;

public class Chat implements MessageListener {
	private TopicSession pubSession;
	private TopicPublisher publisher;
	private TopicConnection connection;
	private String username;

	/* Construtor usado para inicializar o cliente JMS do Chat */
	public Chat(String topicFactory, String topicName, String username) throws Exception {
		// Obtém os dados da conexão JNDI através do arquivo jndi.properties
		InitialContext ctx = new InitialContext();
		// O cliente utiliza o TopicConnectionFactory para criar um objeto do tipo
		// TopicConnection com o provedor JMS
		TopicConnectionFactory conFactory = (TopicConnectionFactory) ctx.lookup(topicFactory);
		// Utiliza o TopicConnectionFactory para criar a conexão com o provedor JMS
		TopicConnection connection = conFactory.createTopicConnection();
		// Utiliza o TopicConnection para criar a sessão para o produtor
		// Atributo false -> uso ou não de transações(tratar uma série de
		// envios/recebimentos como unidade atômica e
		// controlá-la via commit e rollback)
		// Atributo AUTO_ACKNOWLEDGE -> Exige confirmação automática após recebimento
		// correto
		TopicSession pubSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
		// Utiliza o TopicConnection para criar a sessão para o consumidor
		TopicSession subSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
		// Pesquisa o destino do tópico via JNDI
		Topic chatTopic = (Topic) ctx.lookup(topicName);
		// Cria o tópico JMS do produtor das mensagens através da sessão e o nome do
		// tópico
		TopicPublisher publisher = pubSession.createPublisher(chatTopic);
		// Cria(Assina) o tópico JMS do consumidor das mensagens através da sessão e o
		// nome do tópico
		TopicSubscriber subscriber = subSession.createSubscriber(chatTopic);
		// Escuta o tópico para receber as mensagens através do método onMessage()
		subscriber.setMessageListener(this);
		// Inicializa as variaveis do Chat
		this.connection = connection;
		this.pubSession = pubSession;
		this.publisher = publisher;
		this.username = username;
		// Inicia a conexão JMS, permite que mensagens sejam entregues
		connection.start();
	}

	// Recebe as mensagens do tópico assinado
	public void onMessage(Message message) {
		try {
			// As mensagens criadas como TextMessage devem ser recebidas como TextMessage
			TextMessage textMessage = (TextMessage) message;
			System.out.println(textMessage.getText());
		} catch (JMSException jmse) {
			jmse.printStackTrace();
		}
	}
	// Cria a mensagem de texto e a publica no tópico. Evento referente ao produtor

	public void writeMessage(String text) throws JMSException {
		// Recebe um objeto da sessao para criar uma mensagem do tipo TextMessage
		TextMessage message = pubSession.createTextMessage();
		// Seta no objeto a mensagem que será enviada
		message.setText(username + ": " + text);
		// Publica a mensagem no tópico
		publisher.publish(message);
	}

	// Fecha a conexão JMS
	public void close() throws JMSException {
		connection.close();
	}

	// Roda o Chat
	public static void main(String[] args) {
		try {
			// Habilita o envio de mensagens por linha de comando
			Scanner commandLine = new Scanner(System.in);
			System.out.print("Digite seu nome: ");
			String name = commandLine.nextLine();
			// Faz uma chamada ao construtor da classe para iniciar o chat
			Chat chat = new Chat("TopicCF", "topicChat", name);
			// Depois da conexão criada, faz um loop para enviar mensagens
			while (true) {
				// captura a mensagem digitada no console
				String s = commandLine.nextLine();
				// para sair digite exit
				if (s.equalsIgnoreCase("exit")) {
					// fecha a conexão com o provedor
					chat.close();
					// sai do sistema
					System.exit(0);
				} else {
					// envia a mensagem digitada no console para o método writeMessage que irá
					// publica-la
					chat.writeMessage(s);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}