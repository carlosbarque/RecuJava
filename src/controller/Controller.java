package controller;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;

import dao.DaoImpl;
import model.Card;
import model.Player;
import model.Players;
import utils.Color;

import utils.Number;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

/**
 * 
 */
/**
 * 
 */
public class Controller {
	private static Controller controller;
	private DaoImpl dao;
	private Player player;
	private ArrayList<Card> cards;
	private Scanner s;
	private Card lastCard;

	private Controller () {
		dao = new DaoImpl();
		s = new Scanner(System.in);
	}
	
	public static Controller getInstance() {
		if (controller == null) {
			controller = new Controller();
		}
		return controller;
	}

	
	/**
	 * Start game,
	 * connect to db
	 * check user/pw
	 * play a card
	 */
	public void init() {
		try {
			
			// connect to data
			dao.connect();
			
			// connect to data
			// if login ok
			if (loginUser()) {
				
				
				// connect to data
				// check last game
				startGame();
				
			
				// connect to data
				// play turn based on cards in hand
				playTurn();
				
				// connect to data
				
			} else
				System.out.println("User or password incorrect.");
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				// disconnect data
				dao.disconnect();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Show cards in hand
	 * Ask for player action
	 * @throws SQLException
	 */
	private void playTurn() throws SQLException {
		Card card = null;
		boolean correctCard= false;
		boolean end=false;
		
		// loop until option end game option or no more cards
		while (!end) {
			// loop until selected card matches rules based on last card on table
			do {				
				showCards();				
				System.out.println("Press -1 to take a new one.");
				System.out.println("Press -2 to exit game.");
				System.out.println("Press -3 to go to the RecuMenu.");
				int position=0;
				int positionMenu=0;
				
				do {
					System.out.println("Select card to play.");
					position = s.nextInt();
					if (position>=cards.size()) {
						System.out.println("Card does not exist in your hand, try again.");
					}
				} while (position>=cards.size());
								
				switch (position) {
				case -3:
				    positionMenu = 0;
				    do {
				        System.out.println("Select an option:");
				        System.out.println("1. Save players data in file (Option F)");
				        System.out.println("2. Save players data in XML with DOM");
				        System.out.println("3. Save players data in XML with JAXB");
				        System.out.println("Enter your choice:");
				        positionMenu = s.nextInt();
				    } while (positionMenu < 1 || positionMenu > 3);
				    
				    switch (positionMenu) {
				        case 1:
				            savePlayerDataToFile();
				            break;
				        case 2:
				        	savePlayerDataInXML();
				            break;
				        case 3:
				        	savePlayerDataInXMLUsingJAXB();
				    }
				    break;


				case -2:
					correctCard = true;
					end = true;
					System.out.println("Exiting game by pressing EXIT OPTION");
					break;

				case -1:
					drawCards(1);
					break;

				default:
					card = selectCard(position);
					System.out.println("Carta elegida:");
					System.out.println(card);
					correctCard = validateCard(card);
					
					// if skip or change side, remove it and finish game
					if (correctCard) {
						if (card.getNumber().equalsIgnoreCase(Number.SKIP.toString())
								|| card.getNumber().equalsIgnoreCase(Number.CHANGESIDE.toString())) {
							// remove from hand
							this.cards.remove(card);
							dao.deleteCard(card);
							// to end game
							end = true;
							System.out.println("Exiting game by EXIT CARD");
							break;
						}
					}

					// if correct card and no exit card
					if (correctCard && !end) {
						System.out.println("Well done, next turn");
						lastCard = card;
						// save card in game data
						dao.saveGame(card);
						// remove from hand
						this.cards.remove(card);

					} else {
						System.out.println("This card does not match the rules, try other card or draw the deck");
					}
					break;
				}
			} while (!correctCard);
			
			// if no more cards, ends game
			if (this.cards.size() == 0) {
				endGame();
				end=true;
				System.out.println("Exiting game, no more cards, you win.");
				break;
			}			
		}		
	}

	/**
	 * @param card to be played
	 * @return true if it is right based on last card
	 */
	private boolean validateCard(Card card) {
		if (lastCard != null) {
			// same color than previous one
			if (lastCard.getColor().equalsIgnoreCase(card.getColor())) return true;
			// same number than previous one
			if (lastCard.getNumber().equalsIgnoreCase(card.getNumber())) return true;
			// last card is black, it does not matter color
			if (lastCard.getColor().equalsIgnoreCase(Color.BLACK.name())) return true;
			// current card is black, it does not matter color
			if (card.getColor().equalsIgnoreCase(Color.BLACK.name())) return true;
			
			return false;
		} else {
			return true;
		}
	}

	/**
	 * add a new win game
	 * add a new played game
	 * @throws SQLException
	 */
	private void endGame() throws SQLException {
		dao.addVictories(player.getId());
		dao.addGames(player.getId());
		dao.clearDeck(player.getId());
	}

	private Card selectCard(int id) {
		Card card = this.cards.get(id);
		return card;
	}

	private void showCards() {
		System.out.println("================================================");
		if (null == lastCard) {
			System.out.println("First time playing, no cards on table");
		} else {
			System.out.println("Card on table is " + lastCard.toString());
		}
		System.out.println("================================================");
		System.out.println("Your " + cards.size() + " cards in your hand are ...");
		for (int i = 0; i < cards.size(); i++) {
			System.out.println(i + "." + cards.get(i).toString());
		}
	}

	/**
	 * @return true if user/pw found
	 * @throws SQLException
	 */
	private boolean loginUser() throws SQLException {
		System.out.println("Welcome to UNO game!!");
		System.out.println("Name of the user: ");
		String user = s.next();
		System.out.println("Password: ");
		String pass = s.next();
		
		player = dao.getPlayer(user, pass);
		
		if (player != null) {
			return true;
		}
		return false;		
	}
	
	
	/**	 
	 * @throws SQLException
	 */
	private void startGame() throws SQLException {
		// get last cards of player
		
		cards = dao.getCards(player.getId());
		System.out.println("carlitus");
		
		// if no cards, first game, take 3 cards
		if (cards.size() == 0) drawCards(3);
		
		// get last played card
		lastCard = dao.getLastCard();
		
		// for last card +2, take two more
		if (lastCard != null && lastCard.getNumber().equalsIgnoreCase(Number.TWOMORE.toString())) drawCards(2);
		// for last card +4, take four more
		if (lastCard != null && lastCard.getNumber().equalsIgnoreCase(Number.FOURMORE.toString())) drawCards(4);
	}
	
	
	/**
	 * get a number of cards from deck adding them to hand of player
	 * @param numberCards
	 * @throws SQLException
	 */
	private void drawCards(int numberCards) throws SQLException {
		
		for (int i = 0; i < numberCards; i++) {
			int id = dao.getLastIdCard(player.getId());
			
			// handle depends on number color must be black or random
			String number = Number.getRandomCard();
			String color="";
			if (number.equalsIgnoreCase(Number.WILD.toString())|| number.equalsIgnoreCase(Number.FOURMORE.toString())){
				color = Color.BLACK.toString();
			}else {
				color = Color.getRandomColor();
			}
					
			Card c = new Card(id, number , color , player.getId());
			
			dao.saveCard(c);
			cards.add(c);
		}
	}
	
	
	
	//RECUPERACION
	
	
	
	//OPTION FILE
	
	private void savePlayerDataToFile() {
        dao.savePlayersDataToFile();
    }
	
	
	//OPTION DOM
	
	private void savePlayerDataInXML() {
        try {
            // Crear un nuevo documento XML
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();

            // Crear el elemento raíz
            Element rootElement = doc.createElement("Players");
            doc.appendChild(rootElement);

            
            for (Player player : dao.getAllPlayers()) {
                Element playerElement = doc.createElement("Player");
                rootElement.appendChild(playerElement);

                Element idElement = doc.createElement("ID");
                idElement.appendChild(doc.createTextNode(String.valueOf(player.getId())));
                playerElement.appendChild(idElement);

                Element nameElement = doc.createElement("Name");
                nameElement.appendChild(doc.createTextNode(player.getName()));
                playerElement.appendChild(nameElement);

                Element gamesElement = doc.createElement("Games");
                gamesElement.appendChild(doc.createTextNode(String.valueOf(player.getGames())));
                playerElement.appendChild(gamesElement);

                Element victoriesElement = doc.createElement("Victories");
                victoriesElement.appendChild(doc.createTextNode(String.valueOf(player.getVictories())));
                playerElement.appendChild(victoriesElement);
            }

            // Guardar el documento XML en un archivo
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult("files/players_dataDom.xml");
            transformer.transform(source, result);

            System.out.println("Player data saved in XML successfully.");
        } catch (ParserConfigurationException | TransformerException | SQLException e) {
            System.out.println("Error occurred while saving player data to XML: " + e.getMessage());
        }
    }
	
	
	
	
	
	
	
	private void savePlayerDataInXMLUsingJAXB() throws SQLException {
	    try {
	        // Crear contexto JAXB para la clase Players
	        JAXBContext context = JAXBContext.newInstance(Players.class);

	        // Crear un objeto Marshaller
	        Marshaller marshaller = context.createMarshaller();

	        // Formatear la salida XML
	        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

	        // Obtener los datos de los jugadores y guardarlos en un archivo XML
	        ArrayList<Player> players = dao.getAllPlayers();
	        Players playersWrapper = new Players(players);

	        File file = new File("files/players_data_jaxb.xml");
	        marshaller.marshal(playersWrapper, file);

	        System.out.println("Player data saved in XML using JAXB successfully.");
	    } catch (JAXBException e) {
	        System.out.println("Error occurred while saving player data to XML using JAXB: " + e.getMessage());
	    }
	}

	
	


}
