package io.opensaber.registry.test;

import java.util.ArrayList;
import java.util.List;

public class ClientExceptions {

	private boolean exceptException;
	private List<Throwable> exceptions = new ArrayList<>();

	public void expectException() {
		exceptException = true;
	}

	public boolean isExceptException() {
		return exceptException;
	}

	public void add(Throwable t) {
		if (!exceptException) {
			exceptions.add(t);
		}
	}

	public List<Throwable> getExceptions() {
		return exceptions;
	}
}
