package info.ryankenney.jasync_driver.example;

import info.ryankenney.jasync_driver.JasyncDriver;
import info.ryankenney.jasync_driver.AsyncTask;
import info.ryankenney.jasync_driver.DriverBody;
import info.ryankenney.jasync_driver.ResultHandler;
import info.ryankenney.jasync_driver.SyncTask;

import java.util.ArrayList;
import java.util.List;

public class ExampleExternalDataAccessProblem {

	static interface User {
		public String getName();
	}
	
	static class Permissions {
		public List<String> permissions = new ArrayList<>();
	}

	static interface WebServer {
		public void readUserPermissions(User  user, ReturnCallback<Permissions> permission);

		public void storeValue(String value,  ReturnCallback<Status> returnCallback);
	}

	static interface CheckboxWidget {
		public boolean isSet();
	}

	static interface UserInterface {
		public void showError(String message);
		public void promptForNewValue(ReturnCallback<String> returnCallback);
	}

	static interface ReturnCallback<R> {
		public void handleResult(R result);
	}
	
	static enum Status {
		OK,
		FAILURE
	}
	
	WebServer webServer;
	UserInterface userInterface;
	User user;
	CheckboxWidget updateServerValueCheckbox;
	
	private boolean hasEditPermission(Permissions permissions) {
		return false;
	}

	public void onUserClick() {
		
		final AsyncTask<User,Permissions> readUserPermissions = new AsyncTask<User,Permissions>() {
			public void run(final User user, final ResultHandler<Permissions> resultHandler) {
				webServer.readUserPermissions(user, new ReturnCallback<Permissions> () {
					public void handleResult(Permissions result) {
						resultHandler.reportComplete(result);
					}
				});
			}
		};

		final SyncTask<Permissions,Boolean> hasEditPermission = new SyncTask<Permissions,Boolean>() {
			public Boolean run(Permissions permissions) {
				return permissions.permissions.contains("edit");
			}
		};

		final SyncTask<Void,Void> notifyPermissionsError = new SyncTask<Void,Void>() {
			public Void run(Void arg) {
				userInterface.showError("User does not have edit permission");
				return null;
			}
		};

		final AsyncTask<Void,String> promptUserForNewValue = new AsyncTask<Void,String>() {
			public void run(final Void  arg, final ResultHandler<String> resultHandler) {
				userInterface.promptForNewValue(new ReturnCallback<String> () {
					public void handleResult(String result) {
						resultHandler.reportComplete(result);
					}
				});
			}
		};

		final AsyncTask<String,Status> updateStoredValue = new AsyncTask<String,Status>() {
			public void run(final String value, final ResultHandler<Status> resultHandler) {
				webServer.storeValue(value, new ReturnCallback<Status> () {
					public void handleResult(Status result) {
						resultHandler.reportComplete(result);
					}
				});
			}
		};

		final SyncTask<Void,Void> notifyStoreError = new SyncTask<Void,Void>() {
			public Void run(Void arg) {
				userInterface.showError("Store action failed");
				return null;
			}
		};

		final JasyncDriver driver = new JasyncDriver();
		driver.execute(new DriverBody() {
			public void run() {
				Permissions permissions = driver.execute(readUserPermissions, user);
				// ERROR: Accessing a class variable! (defined outside of the DriverBody method)
				// This needs to be wrapped in a SyncTask
				if (updateServerValueCheckbox.isSet()) {
					// WARNING: This method may or may access external variables
					// within. We can't tell from here, so it's safest to wrap
					// it in a SyncTask to ensure that edits to the method body do
					// not break the DriverBody.
					if (!hasEditPermission(permissions)) {
						driver.execute(notifyPermissionsError);
					} else {
						String userInput = driver.execute(promptUserForNewValue);
						Status storeStatus = driver.execute(updateStoredValue, userInput);
						if (storeStatus != Status.OK) {
							driver.execute(notifyStoreError);
						}
					}
				}
			}
		});
	}

}
