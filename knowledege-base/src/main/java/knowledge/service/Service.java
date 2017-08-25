package knowledge.service;

public interface Service<T> {

    public Iterable<T> findAll();

    public T find(Long id);

    public void delete(Long id);

    public T createOrUpdate(T object);

}
