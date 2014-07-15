package com.actionsmicro.pigeon;

import java.util.HashMap;

import android.os.Build;

public class Pigeon {
	private static class Triode {
		private String version;
		private String serverAddress;
		private int portNumber;
		Triode(final String version, final String serverAddress, int portNumber) {
			if(version == null){
				this.version = "1";
			}else{
				this.version = version;
			}
			this.serverAddress = serverAddress;
			this.portNumber = portNumber;
		}
		@Override 
		public boolean equals(Object o) {
			// Return true if the objects are identical.
			// (This is just an optimization, not required for correctness.)
			if (this == o) {
				return true;
			}

			// Return false if the other object has the wrong type.
			// This type may be an interface depending on the interface's specification.
			if (!(o instanceof Triode)) {
				return false;
			}

			// Cast to the appropriate type.
			// This will succeed because of the instanceof, and lets us access private fields.
			Triode lhs = (Triode) o;

			// Check each field. Primitive fields, reference fields, and nullable reference
			// fields are all treated differently.
			return portNumber == lhs.portNumber &&
					(serverAddress == null ? lhs.serverAddress == null
					: serverAddress.equals(lhs.serverAddress)) &&
					(version == null ? lhs.version == null
					: version.equals(lhs.version));
		}
		@Override 
		public int hashCode() {
			// Start with a non-zero constant.
			int result = 430;
			// Include a hash for each field.
			result = 31 * result + portNumber;
			result = 31 * result + (serverAddress == null ? 0 : serverAddress.hashCode());
			result = 31 * result + (version == null ? 0 : version.hashCode());

			return result;
		}
	}
	private static HashMap<Client, Integer> referenceCount = new HashMap<Client, Integer>(); 
	private static HashMap<Triode, Client> clients = new HashMap<Triode, Client>();
	public static Client createPigeonClient(final String version, final String serverAddress, int portNumber) {
		final Triode triode = new Triode(version, serverAddress, portNumber);
		Client pigeon = clients.get(triode);
		if (pigeon == null) {
			if (version != null) {
				if (version.equals("2")) {
					pigeon = new ClientV2(serverAddress, portNumber, Build.MODEL);
				} else {
					pigeon = new Client(serverAddress, portNumber);				
				}
			}
			if (pigeon == null) {
				pigeon = new Client(serverAddress, portNumber);
			}
			clients.put(new Triode(pigeon.getVersion(), pigeon.getServerAddress(), pigeon.getPortNumber()), pigeon);
			//clients.put(triode, pigeon);
			referenceCount.put(pigeon, 1);
		} else {
			referenceCount.put(pigeon, referenceCount.get(pigeon) + 1);
		}
		return pigeon;
	}
	public static void releasePigeonClient(Client pigeon) {
		if (referenceCount.containsKey(pigeon)) {
			int refCount = referenceCount.get(pigeon) - 1;
			if (refCount == 0) {
				clients.remove(new Triode(pigeon.getVersion(), pigeon.getServerAddress(), pigeon.getPortNumber()));
				pigeon.stop();
				referenceCount.remove(pigeon);
			} else {
				referenceCount.put(pigeon, refCount);
			}
		}
	}
}
