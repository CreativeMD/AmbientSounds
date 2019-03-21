package com.creativemd.ambientsounds.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

public class PairList<K, V> extends ArrayList<Pair<K, V>> {
	
	public PairList() {
		super();
	}
	
	public PairList(List<Pair<K, V>> list) {
		super(list);
		updateEntireMap();
	}
	
	protected HashMap<K, Integer> keyIndex = new HashMap<>();
	protected List<V> values = new ArrayList<>();
	
	protected void updateEntireMap() {
		keyIndex.clear();
		values.clear();
		
		for (int i = 0; i < size(); i++) {
			keyIndex.put(this.get(i).key, i);
			values.add(this.get(i).value);
		}
	}
	
	@Override
	public boolean add(Pair<K, V> e) {
		Objects.requireNonNull(e);
		
		if (keyIndex.containsKey(e.key))
			throw new IllegalArgumentException("Duplicates are not allowed key: " + e.key);
		
		if (super.add(e)) {
			keyIndex.put(e.key, size() - 1);
			values.add(e.value);
			return true;
		}
		return false;
	}
	
	@Override
	public void add(int index, Pair<K, V> element) {
		Objects.requireNonNull(element);
		
		if (keyIndex.containsKey(element.key))
			throw new IllegalArgumentException("Duplicates are not allowed key: " + element.key);
		
		super.add(index, element);
		updateEntireMap();
	}
	
	@Override
	public boolean addAll(Collection<? extends Pair<K, V>> c) {
		Objects.requireNonNull(c);
		
		for (Pair<K, V> pair : c) {
			Objects.requireNonNull(pair);
			
			if (keyIndex.containsKey(pair.key))
				throw new IllegalArgumentException("Duplicates are not allowed key: " + pair.key);
		}
		
		int sizeBefore = size();
		if (super.addAll(c)) {
			for (int i = 0; i < c.size(); i++) {
				int index = sizeBefore + i;
				keyIndex.put(this.get(index).key, index);
				values.add(this.get(index).value);
			}
			return true;
		}
		return false;
	}
	
	@Override
	public boolean addAll(int index, Collection<? extends Pair<K, V>> c) {
		Objects.requireNonNull(c);
		
		for (Pair<K, V> pair : c) {
			Objects.requireNonNull(pair);
			
			if (keyIndex.containsKey(pair.key))
				throw new IllegalArgumentException("Duplicates are not allowed key: " + pair.key);
		}
		
		if (super.addAll(index, c)) {
			updateEntireMap();
			return true;
		}
		return false;
	}
	
	public boolean add(K key, V value) {
		return add(new Pair<>(key, value));
	}
	
	public void set(K key, V value) {
		Pair<K, V> pair = getPair(key);
		if (pair != null)
			pair.value = value;
	}
	
	@Override
	public Pair<K, V> remove(int index) {
		Pair<K, V> pair;
		if ((pair = super.remove(index)) != null) {
			updateEntireMap();
			return pair;
		}
		return null;
	}
	
	@Override
	public boolean remove(Object o) {
		if (super.remove(o)) {
			updateEntireMap();
			return true;
		}
		return false;
	}
	
	@Override
	public boolean removeAll(Collection<?> c) {
		if (super.removeAll(c)) {
			updateEntireMap();
			return true;
		}
		return false;
	}
	
	@Override
	public boolean removeIf(Predicate<? super Pair<K, V>> filter) {
		if (super.removeIf(filter)) {
			updateEntireMap();
			return true;
		}
		return false;
	}
	
	@Override
	public boolean retainAll(Collection<?> c) {
		if (super.retainAll(c)) {
			updateEntireMap();
			return true;
		}
		return false;
	}
	
	@Override
	public void clear() {
		super.clear();
		keyIndex.clear();
		values.clear();
	}
	
	@Override
	public Pair<K, V> set(int index, Pair<K, V> element) {
		Objects.requireNonNull(element);
		
		Integer exisiting = keyIndex.get(element.key);
		if (exisiting != null && exisiting != index)
			throw new IllegalArgumentException("Duplicates are not allowed key: " + element.key);
		
		Pair<K, V> old = super.set(index, element);
		if (old != null) {
			keyIndex.remove(old.key);
			keyIndex.put(element.key, index);
			values.set(index, element.value);
		}
		return old;
	}
	
	@Override
	public void sort(Comparator<? super Pair<K, V>> c) {
		super.sort(c);
		updateEntireMap();
	}
	
	public boolean containsKey(K key) {
		return keyIndex.containsKey(key);
	}
	
	public int indexOfKey(K key) {
		return keyIndex.getOrDefault(key, -1);
	}
	
	public boolean removeKey(K key) {
		Integer index = keyIndex.get(key);
		if (index != null)
			return remove((int) index) != null;
		return false;
	}
	
	public List<V> values() {
		return ImmutableList.copyOf(values);
	}
	
	public Set<K> keys() {
		return keyIndex.keySet();
	}
	
	public Pair<K, V> getFirst() {
		if (isEmpty())
			return null;
		return get(0);
	}
	
	public Pair<K, V> getLast() {
		if (isEmpty())
			return null;
		return get(size() - 1);
	}
	
	@Nullable
	public V getValue(K key) {
		Integer index = keyIndex.get(key);
		if (index != null)
			return get((int) index).value;
		return null;
	}
	
	@Nullable
	public Pair<K, V> getPair(K key) {
		Integer index = keyIndex.get(key);
		if (index != null)
			return get((int) index);
		return null;
	}
	
}
