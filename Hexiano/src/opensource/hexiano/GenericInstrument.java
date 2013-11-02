/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *                                                                         *
 *   Hexiano, an isomorphic musical keyboard for Android                   *
 *   Copyleft  @ 2013 Stephen Larroque                                     *
 *                                                                         *
 *   FILE: GenericInstrument.java                                          *
 *                                                                         *
 *   This file is part of Hexiano, an open-source project hosted at:       *
 *   https://gitorious.org/hexiano                                         *
 *                                                                         *
 *   Hexiano is free software: you can redistribute it and/or              *
 *   modify it under the terms of the GNU General Public License           *
 *   as published by the Free Software Foundation, either version          *
 *   3 of the License, or (at your option) any later version.              *
 *                                                                         *
 *   Hexiano is distributed in the hope that it will be useful,            *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with Hexiano.  If not, see <http://www.gnu.org/licenses/>.      *
 *                                                                         *
 *   NOTE: The sounds used for this instrument were derived from           *
 *   the acoustic-piano soundfont created by Roberto Gordo Saez.
 *   Here is its license, which we will display more prominently
 *   in the application and our web site, as soon as we get 
 *   organized:
 *   
Acoustic grand piano soundfont (Yamaha Disklavier Pro), release 2008-09-10
116 samples, 44100Hz, 16bit.

The acoustic grand piano soundfont is free. It is built from the Zenph
Studios Yamaha Disklavier Pro Piano Multisamples for OLPC.

The soundfont itself and all modifications made to the original
samples by Roberto Gordo Saez, published under a Creative Commons
Attribution 3.0 license.

Copyright 2008, Roberto Gordo Saez roberto.gordo@gmail.com Creative 
Commons Attribution 3.0 license http://creativecommons.org/licenses/by/3.0/

Zenph Studios Yamaha Disklavier Pro Piano Multisamples for OLPC:

A collection of Grand Piano samples played by a Yamaha Disklavier
Pro. Performed by computer and specifically recorded for OLPC by
Dr. Mikhail Krishtal, Director of Music Research and Production, and
his team at Zenph Studios. They are included in the OLPC sound
sample library.

How is it being done: "The Disklavier Pro has an internal
electronically-controlled mechanism that allows it to play sounds
with very precise specifications. It has its own file format known
as XP MIDI, an extension of standard midi. I Mikhail Krishtal
prepare the files for it to play -- in this case, representing notes
of different registers, durations, and dynamic levels."

http://csounds.com/olpc/pianoSamplesMikhail/pianoMikhail.html
Produced by Zenph Studios in Chapel Hill, North Carolina. The main
studio location is in Raleigh, North Carolina.

http://zenph.com/
Samples from the OLPC sound sample library:

This huge collection of new and original samples have been donated
to Dr. Richard Boulanger @ cSounds.com specifically to support the
OLPC developers, students, XO users, and computer and electronic
musicians everywhere. They are FREE and are offered under a CC-BY
license.

http://wiki.laptop.org/go/Sound_samples http://csounds.com/boulanger

Creative Commons Attribution 3.0 license
http://creativecommons.org/licenses/by/3.0/

* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package opensource.hexiano;

import java.io.File;
import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class GenericInstrument extends Instrument
{

	public GenericInstrument(Context context, String instrument) throws IllegalAccessException
	{
		super(context);
		
		this.mExternal = true;

		Pattern pat = Pattern.compile("([0-9]+)\\.[^\\.]*$"); // Pattern: detect the midi note in the last number just before the extension
		File[] files = listExternalFiles(instrument+"/"); // Get the list of all files for this instrument
		List<ArrayList> sounds = new ArrayList<ArrayList>();
		// For each file
		for (File file : files)
		{
		    String fileName = file.getName();
		    Log.d("GenericInstrument", "Found file: "+fileName);
		    // If we find a midi note (matching the regexp)
			int midiNoteNumber;
			Matcher mat = pat.matcher(fileName);
			if (mat.find())
			{
				// Parse the filename string to get the midi note
				String midiNoteNumberStr = mat.group(1);
				midiNoteNumber = Integer.parseInt(midiNoteNumberStr);
				String filePath = file.getAbsolutePath();
				Log.d("GenericInstrument", "Found midi note: "+midiNoteNumberStr + ": " + filePath);
				// And store it inside the array sounds with the midiNoteNumber being the id and filePath the resource to load
				ArrayList tuple = new ArrayList();
				tuple.add(midiNoteNumber);
				tuple.add(filePath);
				sounds.add(0, tuple);
				// Also set this note as a root note (Root Notes are notes we have files for, from which we will extrapolate other notes that are missing if any)
				mRootNotes.put(midiNoteNumber, midiNoteNumber);
				// Rate to play the file with, default is always used for now
				mRates.put(midiNoteNumber, 1.0f);
			}
		}

		// Create an iterator to generate all sounds of all notes
		sound_load_queue = sounds.iterator();
		// Start loading the first sound, the rest are started from the Play::OnLoadCompleteListener()
		ArrayList tuple = sound_load_queue.next();
		addSound((Integer)tuple.get(0), (String)tuple.get(1));

		// Extrapolate missing notes from Root Notes (notes for which we have a sound file)
		float previousRate = 1.0f;
		int previousRootNote = 0;
		for (int noteId = 0; noteId < 128; noteId++)
		{
			if (mRootNotes.containsKey(noteId))
			{
				previousRootNote = noteId;
				previousRate = 1.0f;
			}
			else
			{
				mRootNotes.put(noteId, previousRootNote);
				double oneTwelfth = 1.0/12.0;
			    double newRate = previousRate * Math.pow(2, oneTwelfth);	
			    previousRate = (float)newRate;
				mRates.put(noteId, previousRate);
			}
		}
	}

	// List all external (eg: sd card) instruments (just the name of the subfolders)
	public static ArrayList<String> listExternalInstruments() {
		return GenericInstrument.listExternalInstruments("/hexiano/");
	}

	// List all external instruments (just the name of the subfolders)
	public static ArrayList<String> listExternalInstruments(String path) {
		ArrayList<String> Directories = new ArrayList<String>();
		if (path.length() != 0) {
			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				path = Environment.getExternalStorageDirectory().toString()+path;
				Log.d("GenericInstrument::listExternalInstruments", "GenericInstrument: list external instruments (directories) from path: "+path);

			    File f = new File(path);
			    File[] files = f.listFiles();
			    for (File file : files) {
			        if (file.isDirectory()) { // is directory
			        	Directories.add(file.getName());
			        }
			    }
			}
		}
	    return Directories;
	}

	// List all files in the external instrument's folder
	public static File[] listExternalFiles(String path) {
		File[] Files = null;
		if (path.length() != 0) {
			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				path = Environment.getExternalStorageDirectory().toString()+"/hexiano/"+path;
				Log.d("GenericInstrument::listExternalFiles", "GenericInstrument: list external files from path: "+path);

			    File f = new File(path);
			    Files = f.listFiles();
			}
		}
		return Files;
	}

}
