package search.common.mongo.base;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class ReflectNameChain {
	
	private String nameChain;
	private Method method;
	private Object obj;
	private Type fType;
	public String getNameChain() {
		return nameChain;
	}
	public void setNameChain(String nameChain) {
		this.nameChain = nameChain;
	}
	public Method getMethod() {
		return method;
	}
	public void setMethod(Method method) {
		this.method = method;
	}
	public Object getObj() {
		return obj;
	}
	public void setObj(Object obj) {
		this.obj = obj;
	}
	public Type getfType() {
		return fType;
	}
	public void setfType(Type fType) {
		this.fType = fType;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fType == null) ? 0 : fType.hashCode());
		result = prime * result + ((method == null) ? 0 : method.hashCode());
		result = prime * result
				+ ((nameChain == null) ? 0 : nameChain.hashCode());
		result = prime * result + ((obj == null) ? 0 : obj.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReflectNameChain other = (ReflectNameChain) obj;
		if (fType == null) {
			if (other.fType != null)
				return false;
		} else if (!fType.equals(other.fType))
			return false;
		if (method == null) {
			if (other.method != null)
				return false;
		} else if (!method.equals(other.method))
			return false;
		if (nameChain == null) {
			if (other.nameChain != null)
				return false;
		} else if (!nameChain.equals(other.nameChain))
			return false;
		if (this.obj == null) {
			if (other.obj != null)
				return false;
		} else if (!this.obj.equals(other.obj))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "ReflectNameChain [nameChain=" + nameChain + ", method="
				+ method + ", obj=" + obj + ", fType=" + fType + "]";
	}

}
