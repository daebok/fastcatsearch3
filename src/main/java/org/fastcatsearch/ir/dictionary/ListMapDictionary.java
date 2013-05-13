package org.fastcatsearch.ir.dictionary;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.fastcatsearch.ir.io.CharVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListMapDictionary extends SourceDictionary implements ReadableDictionary {
	private static Logger logger = LoggerFactory.getLogger(ListMapDictionary.class);

	private Map<CharVector, CharVector[]> map;
	private Set<CharVector> synonymSet;

	public ListMapDictionary() {
		map = new HashMap<CharVector, CharVector[]>();
		synonymSet = new HashSet<CharVector>();
	}

	public ListMapDictionary(Map<CharVector, CharVector[]> map) {
		this.map = map;
	}

	public ListMapDictionary(File file) {
		if(!file.exists()){
			map = new HashMap<CharVector, CharVector[]>();
			synonymSet = new HashSet<CharVector>();
			logger.error("사전파일이 존재하지 않습니다. file={}", file.getAbsolutePath());
			return;
		}
		InputStream is = null;
		try {
			is = new FileInputStream(file);
			readFrom(is);
			is.close();
		} catch (IOException e) {
			logger.error("", e);
		}
	}

	public ListMapDictionary(InputStream is) {
		try {
			readFrom(is);
		} catch (IOException e) {
			logger.error("", e);
		}
	}

	@Override
	public void addEntry(String line) {
		ArrayList<CharVector> list = new ArrayList<CharVector>(4);

		String[] synonyms = line.split(",");
		if (synonyms.length > 1) {
			CharVector mainWord = null;
			for (int k = 0; k < synonyms.length; k++) {
				String synonym = synonyms[k].trim();
				if (synonym.length() > 0) {
					if (synonym.startsWith("@")) {
						mainWord = new CharVector(synonym.substring(1));
						synonymSet.add(mainWord);
					} else {
						CharVector word = new CharVector(synonym);
						list.add(word);
						synonymSet.add(word);
					}
				}
			}

			if (mainWord == null) {
				// 양방향.
				for (int j = 0; j < list.size(); j++) {
					CharVector key = list.get(j);
					CharVector[] value = new CharVector[list.size() - 1];
					int idx = 0;
					for (int k = 0; k < list.size(); k++) {
						CharVector val = list.get(k);
						if (!key.equals(val)) {
							// 다른것만 value로 넣는다.
							value[idx++] = val;
						}
					}
					map.put(key, value);
					logger.debug("유사어 양방향 {} >> {} {}", key, value[0], value[1]);
				}
			} else {
				// 단방향.
				CharVector[] value = new CharVector[list.size()];
				for (int j = 0; j < value.length; j++) {
					value[j] = list.get(j);
				}
				map.put(mainWord, value);
			}
		}
	}

	public Map<CharVector, CharVector[]> getMap() {
		return Collections.unmodifiableMap(map);
	}

	public Set<CharVector> getWordSet() {
		return Collections.unmodifiableSet(synonymSet);
	}

	@Override
	public void writeTo(OutputStream out) throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(out);
		oos.writeObject(map);
		oos.writeObject(synonymSet);
	}

	@Override
	public void readFrom(InputStream in) throws IOException {
		ObjectInputStream ois = new ObjectInputStream(in);
		try {
			map = (Map<CharVector, CharVector[]>) ois.readObject();
			synonymSet = (Set<CharVector>) ois.readObject();
		} catch (ClassNotFoundException e) {
			throw new IOException(e);
		}
	}

}
