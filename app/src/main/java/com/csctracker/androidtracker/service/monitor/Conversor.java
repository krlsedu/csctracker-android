//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.csctracker.androidtracker.service.monitor;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.List;
import java.util.stream.Collectors;

public class Conversor<E, D> {
    private final ObjectMapper objectMapper;
    private final Class<E> eClass;
    private final Class<D> dClass;

    public Conversor(Class<E> eClass, Class<D> dClass) {
        this.eClass = eClass;
        this.dClass = dClass;
        this.objectMapper = (new ObjectMapper()).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, false).setSerializationInclusion(Include.NON_NULL);
    }

    public static ObjectMapper getObjectMapperStatic() {
        return (new ObjectMapper()).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, false).setSerializationInclusion(Include.NON_NULL);
    }

    public List<E> toE(List<D> ds) {
        return (List) ds.stream().map(this::toE).collect(Collectors.toList());
    }

    public E toE(D d) {
        return this.objectMapper.convertValue(d, this.eClass);
    }

    public List<D> toD(List<E> es) {
        return (List) es.stream().map(this::toD).collect(Collectors.toList());
    }

    public D toD(E e) {
        return this.objectMapper.convertValue(e, this.dClass);
    }

    public List<D> toDList(String d) throws JsonProcessingException {
        return (List) this.objectMapper.readValue(d, this.objectMapper.getTypeFactory().constructCollectionType(List.class, this.dClass));
    }

    public D toD(String d) throws JsonProcessingException {
        return this.objectMapper.readValue(d, this.dClass);
    }

    public D toD(String t, Class clazz) {
        return this.objectMapper.convertValue(t, this.dClass);
    }

    public ObjectMapper getObjectMapper() {
        return this.objectMapper;
    }
}
