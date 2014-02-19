package emoapp;

import java.io.File;

import processing.core.PApplet;
import processing.data.Table;
import processing.data.TableRow;
import controlP5.ControlEvent;

@SuppressWarnings("serial")
public class EmoApp extends PApplet {
	// edk (headset) conn.
	EdkConn ec;
	int connTo = 2;
	// headset data
	float exc = 0, eng = 0, med = 0, frs = 0;
	float smile = 0, clench = 0;
	int blink = 0, winkL = 0, winkR = 0;

	Table emoValuesTbl = new Table();
	// loaded csv table data
	Table loadedValuesTbl = new Table();
	boolean loading = false;
	boolean loaded = false;
	int loadedRowCounter = 0;

	ParticleSphere pSph;
	GUI gui;

	public void setup() {
		size(1200, 800, P3D);
		background(0);
		frameRate(24);

		// Connect to headset
		ec = new EdkConn(this);
		gui = new GUI(this);
		pSph = new ParticleSphere(this);
		pSph.setup();

		// add columns to emoValues table
		emoValuesTbl.addColumn("exc");
		emoValuesTbl.addColumn("eng");
		emoValuesTbl.addColumn("med");
		emoValuesTbl.addColumn("frs");
		emoValuesTbl.addColumn("blink");
		emoValuesTbl.addColumn("smile");
		emoValuesTbl.addColumn("clench");
		emoValuesTbl.addColumn("winkL");
		emoValuesTbl.addColumn("winkR");
	}

	public void initEmoValues() {
		exc = 0;
		eng = 0;
		med = 0;
		frs = 0;
		blink = 0;
		smile = 0;
		clench = 0;
		winkL = 0;
		winkR = 0;
	}

	// Draw is used like in the processing tool.
	public void draw() {
		background(0);
		// live data
		if (ec.connected && !loaded && !loading) {
			// Run headset event listener loop each time draw() is called
			boolean stateChanged = ec.edkRun();
			// avgContactQlty will be more than 2 only when using emocomposer
			// only goes up to 2 with headset
			if (ec.avgContactQlty >= 1 && ec.avgContactQlty <= 4) {
				if (stateChanged) {
					exc = ec.excitement;
					eng = ec.engagement;
					med = ec.meditation;
					frs = ec.frustration;
					blink = ec.blink;
					smile = ec.smile;
					clench = ec.clench;
					winkL = ec.winkLeft;
					winkR = ec.winkRight;
					if (gui.recording) {
						TableRow newRow = emoValuesTbl.addRow();
						newRow.setFloat("exc", exc);
						newRow.setFloat("eng", eng);
						newRow.setFloat("med", med);
						newRow.setFloat("frs", frs);
						newRow.setInt("blink", blink);
						newRow.setFloat("smile", smile);
						newRow.setFloat("clench", clench);
						newRow.setInt("winkL", winkL);
						newRow.setInt("winkR", winkR);
					}
					pSph.draw(exc, eng, med, frs, blink, smile, clench, winkL,
							winkR);
				}
			} else {
				initEmoValues();
			}
			if (gui.reset) {
				emoValuesTbl.clearRows();
				gui.reset = false;
			}
			gui.update(ec.headsetOn, ec.signal, ec.avgContactQlty);
		}
		// loaded data
		if (loading) {
			gui.loadHandler("loading");
		} else if (loaded && !loading) {

			if (loadedRowCounter < loadedValuesTbl.getRowCount()) {

				gui.loadHandler("playing");
				// checkColumnIndex() creates the column if it doesn't exist
				loadedValuesTbl.checkColumnIndex("exc");
				exc = loadedValuesTbl.getFloat(loadedRowCounter, "exc");

				loadedValuesTbl.checkColumnIndex("eng");
				eng = loadedValuesTbl.getFloat(loadedRowCounter, "eng");

				loadedValuesTbl.checkColumnIndex("med");
				med = loadedValuesTbl.getFloat(loadedRowCounter, "med");

				loadedValuesTbl.checkColumnIndex("frs");
				frs = loadedValuesTbl.getFloat(loadedRowCounter, "frs");

				loadedValuesTbl.checkColumnIndex("blink");
				blink = loadedValuesTbl.getInt(loadedRowCounter, "blink");

				loadedValuesTbl.checkColumnIndex("smile");
				smile = loadedValuesTbl.getFloat(loadedRowCounter, "smile");

				loadedValuesTbl.checkColumnIndex("clench");
				clench = loadedValuesTbl.getFloat(loadedRowCounter, "clench");

				loadedValuesTbl.checkColumnIndex("winkL");
				winkL = loadedValuesTbl.getInt(loadedRowCounter, "winkL");

				loadedValuesTbl.checkColumnIndex("winkR");
				winkR = loadedValuesTbl.getInt(loadedRowCounter, "winkR");

				loadedRowCounter++;

			} else {
				loaded = false;
				loadedRowCounter = 0;
				initEmoValues();
				gui.loadHandler("done");
			}
			pSph.draw(exc, eng, med, frs, blink, smile, clench, winkL, winkR);
		}
	}

	public void controlEvent(ControlEvent theEvent) {
		// connect button is handled here and not in GUI
		// in order to avoid creating an instance of EdkConn inside GUI
		if (theEvent.isFrom("connect")) {
			// TODO - add choice
			// if param="1" conn. to headset, "2" conn. to emocomposer
			ec.edkConn(connTo);
		}
		if(theEvent.isFrom("reconnect")){
			if(!ec.connected){
				ec.edkConn(connTo);
				gui.bReconnect.hide();
			}
		}
		if (ec.connError) {
			gui.errorMsg(ec.errorMsg);
		}
		//if (ec.connected) {
			gui.handler(theEvent);
		//} 	
	}

	public void saveToFile(File selection) {
		if (selection == null) {
			println("Window was closed or the user hit cancel.");
		} else {
			String filename = selection.getAbsolutePath();

			if (!filename.endsWith("csv")) {
				filename = filename.concat(".csv");
			}
			// on windows will throw uncatchable exception if file is open
			// while trying to save to it
			// but app will keep working
			saveTable(emoValuesTbl, filename);
		}
	}

	// handles "load" button press, loads saved csv table data
	public void loadFile(File selection) {
		if (selection != null) {
			ec.disconnect();
			try {
				loading = true;
				loadedValuesTbl = loadTable(selection.getAbsolutePath(),
						"header");
				loaded = true;
				gui.nowPlayingFilename = selection.getName();
			} catch (Exception e) {
				gui.errorMsg("No saved data found.");
				// TODO - add choice
				ec.edkConn(connTo);
			}
			loading = false;
		}
	}
	
	public static void main(String _args[]) {
		PApplet.main(new String[] { emoapp.EmoApp.class.getName() });
	}
}
