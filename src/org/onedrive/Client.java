package org.onedrive;

import com.sun.istack.internal.NotNull;
import lombok.Getter;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.network.HttpsRequest;
import org.network.HttpsResponse;
import org.onedrive.container.Drive;
import org.onedrive.container.items.FileItem;
import org.onedrive.container.items.FolderItem;
import org.onedrive.utils.AuthServer;
import org.onedrive.utils.OneDriveRequest;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * {@// TODO: add javadoc}
 *
 * @author <a href="mailto:yoobyeonghun@gmail.com" target="_top">isac322</a>
 */
public class Client {
	private static final String UNREACHABLE_MSG = "Unreachable Area. Check stack stace above";
	private final JSONParser parser = new JSONParser();
	private long lastRefresh;
	@Getter private long expirationTime;
	@Getter private String tokenType;
	@Getter private String accessToken;
	@Getter private String userId;
	@Getter private String refreshToken;
	@Getter private String[] scopes;
	@Getter private String clientId;
	@Getter private String clientSecret;
	@Getter private String redirectURL;
	@Getter private String authCode;

	/**
	 * Construct with auto login.
	 *
	 * @param clientId     Client id that MS gave to programmer for identify programmer's applications.
	 * @param scope        Array of scopes that client requires.
	 * @param redirectURL  Redirect URL that programmer already set in Application setting. It must matches with set
	 *                     one!
	 * @param clientSecret Client secret key that MS gave to programmer.
	 */
	public Client(String clientId, String[] scope, String redirectURL, String clientSecret) {
		this(clientId, scope, redirectURL, clientSecret, true);
	}

	/**
	 * @param clientId     Client id that MS gave to programmer for identify programmer's applications.
	 * @param scope        Array of scopes that client requires.
	 * @param redirectURL  Redirect URL that programmer already set in Application setting. It must matches with set
	 *                     one!
	 * @param clientSecret Client secret key that MS gave to programmer.
	 * @param autoLogin    if {@code true} construct with auto login.
	 */
	public Client(String clientId, String[] scope, String redirectURL, String clientSecret, boolean autoLogin) {
		this.scopes = scope;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.redirectURL = redirectURL;
		if (autoLogin) login();
	}

	/**
	 * Implementation of
	 * <a href=https://dev.onedrive.com/auth/msa_oauth.htm>detail</a>
	 *
	 * @throws RuntimeException if login is unsuccessful.
	 */
	private void login() throws RuntimeException {
		if (!isLogin()) {
			authCode = getCode();
			redeemToken();
		}
	}

	/**
	 * Implementation of
	 * <a href=https://dev.onedrive.com/auth/msa_oauth.htm#step-1-get-an-authorization-code>detail</a>
	 *
	 * @return <b>Access Code</b>({@code this.accessToken}) if successful. Otherwise throw {@link RuntimeException}.
	 * @throws RuntimeException if getting <b>Access Code</b> is unsuccessful.
	 */
	private String getCode() {
		String url = String.format("https://login.live.com/oauth20_authorize.srf" +
						"?client_id=%s&scope=%s&response_type=code&redirect_uri=%s",
				clientId,
				String.join(" ", (CharSequence[]) scopes),
				redirectURL)
				.replace(" ", "%20");

		try {
			Semaphore answerLock = new Semaphore(1);

			AuthServer server = new AuthServer(answerLock);
			server.start();
			Desktop.getDesktop().browse(new URI(url));

			answerLock.acquire();
			String code = server.close();
			answerLock.release();

			return code;
		}
		catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
			System.err.println("Lock Error In " + this.getClass().getName());
		}

		throw new RuntimeException(UNREACHABLE_MSG);
	}

	private String redeemToken() {
		return getToken(
				String.format("client_id=%s&redirect_uri=%s&client_secret=%s&code=%s&grant_type=authorization_code",
						clientId, redirectURL, clientSecret, authCode));
	}

	public String refreshToken() {
		if (!isLogin()) {
			throw new RuntimeException("Do login first!!");
		}

		return getToken(
				String.format("client_id=%s&redirect_uri=%s&client_secret=%s&refresh_token=%s&grant_type" +
								"=refresh_token",
						clientId, redirectURL, clientSecret, refreshToken));
	}

	private String getToken(String httpBody) {
		try {
			HttpsRequest request = new HttpsRequest("https://login.live.com/oauth20_token.srf");
			request.setHeader("Content-Type", "application/x-www-form-urlencoded");

			HttpsResponse response = request.doPost(httpBody);


			JSONObject jsonResponse = (JSONObject) parser.parse(response.getContentString());

			saveToken(
					jsonResponse.getString("access_token"),
					jsonResponse.getString("refresh_token"),
					jsonResponse.getString("user_id"),
					jsonResponse.getString("token_type"),
					jsonResponse.getLong("expires_in")
			);

			return jsonResponse.getString("access_token");

		}
		catch (IOException e) {
			e.printStackTrace();
			System.err.println("Internet Connection Error While Refreshing Login Info");
		}
		catch (ParseException e) {
			e.printStackTrace();
			System.err.println("Response Json Error in " + this.getClass().getName());
		}

		throw new RuntimeException(UNREACHABLE_MSG);
	}

	private void saveToken(String accessToken, String refreshToken, String userId, String type, long expirationTime) {
		this.tokenType = type;
		this.userId = userId;
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.expirationTime = expirationTime * 1000;
		this.lastRefresh = System.currentTimeMillis();
	}

	private void checkExpired() {
		if (isExpired()) {
			refreshToken();
		}
	}

	/**
	 * {@// TODO: check logout HTTP response about error.}
	 *
	 * @throws RuntimeException if it isn't login when called.
	 */
	public void logout() {
		if (!isLogin()) throw new RuntimeException("Already Logout!!");

		try {
			String url = String.format("https://login.live.com/oauth20_logout.srf?client_id=%s&redirect_uri=%s",
					clientId, redirectURL);

			HttpsRequest request = new HttpsRequest(url);
			// TODO
			HttpsResponse response = request.doGet();

			authCode = null;
			accessToken = null;
			userId = null;
			refreshToken = null;
			expirationTime = 0;
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean isExpired() {
		return System.currentTimeMillis() - lastRefresh >= expirationTime;
	}

	public boolean isLogin() {
		return authCode != null && accessToken != null && userId != null && refreshToken != null;
	}

	@NotNull
	public Drive getDefaultDrive() {
		checkExpired();

		try {
			HttpsResponse response = OneDriveRequest.doGet("/drive", accessToken);
			JSONObject json = (JSONObject) parser.parse(response.getContentString());

			return Drive.parse(json);
		}
		catch (ParseException e) {
			e.printStackTrace();
			System.err.println("Wrong json response. It must be connection error");
		}

		throw new RuntimeException(UNREACHABLE_MSG);
	}

	@NotNull
	public List<Drive> getAllDrive() {
		ArrayList<Drive> list = new ArrayList<>();

		JSONObject json = OneDriveRequest.doGetJson("/drives", accessToken);

		for (Object drive : json.getArray("value")) {
			list.add(Drive.parse((JSONObject) drive));
		}

		return list;
	}

	@NotNull
	public FolderItem getRootDir() {
		JSONObject json = OneDriveRequest.doGetJson("/drive/root:/?expand=children", accessToken);

		return (FolderItem) FolderItem.parse(this, json);
	}

	@NotNull
	public FolderItem getFolder(@NotNull String id) {
		try {
			JSONObject json = OneDriveRequest.doGetJson("/drive/items/" + id + "?expand=children", accessToken);

			return (FolderItem) FolderItem.parse(this, json);
		}
		catch (ClassCastException e) {
			e.printStackTrace();
			System.err.println(id + " is not folder ID.");
		}

		throw new RuntimeException(UNREACHABLE_MSG);
	}

	@NotNull
	public FileItem getFile(@NotNull String id) {
		try {
			JSONObject json = OneDriveRequest.doGetJson("/drive/items/" + id + "?expand=children", accessToken);

			return (FileItem) FileItem.parse(this, json);
		}
		catch (ClassCastException e) {
			e.printStackTrace();
			System.err.println(id + " is not file ID.");
		}

		throw new RuntimeException(UNREACHABLE_MSG);
	}
}