/**
 * Copyright © Microsoft Open Technologies, Inc.
 *
 * All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
 * ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A
 * PARTICULAR PURPOSE, MERCHANTABILITY OR NON-INFRINGEMENT.
 *
 * See the Apache License, Version 2.0 for the specific language
 * governing permissions and limitations under the License.
 */
package com.msopentech.odatajclient.proxy.api.impl;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;
import com.microsoft.office.proxy.OfficeEntitySet;
import com.msopentech.odatajclient.engine.communication.ODataClientErrorException;
import com.msopentech.odatajclient.engine.communication.request.retrieve.ODataValueRequest;
import com.msopentech.odatajclient.engine.communication.response.ODataRetrieveResponse;
import com.msopentech.odatajclient.engine.data.ODataEntity;
import com.msopentech.odatajclient.engine.data.ODataEntitySet;
import com.msopentech.odatajclient.engine.data.ODataObjectFactory;
import com.msopentech.odatajclient.engine.format.ODataValueFormat;
import com.msopentech.odatajclient.engine.uri.URIBuilder;
import com.msopentech.odatajclient.proxy.api.AbstractEntityCollection;
import com.msopentech.odatajclient.proxy.api.EntityContainerFactory;
import com.msopentech.odatajclient.proxy.api.Query;
import com.msopentech.odatajclient.proxy.api.annotations.CompoundKey;
import com.msopentech.odatajclient.proxy.api.annotations.CompoundKeyElement;
import com.msopentech.odatajclient.proxy.api.annotations.EntitySet;
import com.msopentech.odatajclient.proxy.api.annotations.EntityType;
import com.msopentech.odatajclient.proxy.api.context.AttachedEntityStatus;
import com.msopentech.odatajclient.proxy.api.context.EntityContext;
import com.msopentech.odatajclient.proxy.api.context.EntityUUID;
import com.msopentech.odatajclient.proxy.utils.ClassUtils;

class EntitySetInvocationHandler<
        T extends Serializable, KEY extends Serializable, EC extends AbstractEntityCollection<T>>
        extends AbstractInvocationHandler
        implements OfficeEntitySet<T, KEY, EC> {

    private static final long serialVersionUID = 2629912294765040027L;

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(EntitySetInvocationHandler.class);

    private final Class<T> typeRef;

    private final Class<EC> collTypeRef;

    private final String entitySetName;

    /**
     * Path for RUD operations.
     */
    private String entitySetPath;

    /**
     * Path for Create operation
     */
    private String createPath;

    private final URI uri;

    /**
     * Represents this set as collection.
     */
    private EntityCollectionInvocationHandler<T> setAsCollection = null;

    /**
     * An instance of iterator to be returned when iterator() method called before entities collected from service.
     */
    private final Iterator<T> mEmptyIterator = new Iterator<T>() {
        @Override
        public boolean hasNext() {
            return false;
        }
        @Override
        public T next() {
            throw new NoSuchElementException("You must retrieve data from service before accessing iterator.");
        }
        @Override
        public void remove() {
            throw new IllegalStateException("You must retrieve data from service before accessing iterator.");
        }
    };

    /**
     * Gets an instance of {@link EntitySetInvocationHandler}.
     *
     * @param ref Entity set java type.
     * @param containerHandler Container handler.
     * @return EntitySetInvocationHandler instance.
     * @throws UnsupportedEncodingException thrown never.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static EntitySetInvocationHandler getInstance(
            final Class<?> ref, final EntityContainerInvocationHandler containerHandler) throws UnsupportedEncodingException {

        return new EntitySetInvocationHandler(ref, containerHandler);
    }

    /**
     * Gets an instance of {@link EntitySetInvocationHandler}.
     *
     * @param ref Entity set java type.
     * @param containerHandler Container handler.
     * @param path Path to entity set related to service root.
     * @return EntitySetInvocationHandler instance
     * @throws UnsupportedEncodingException thrown never.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    static EntitySetInvocationHandler getInstance(
            final Class<?> ref, final EntityContainerInvocationHandler containerHandler, String path) {

        return new EntitySetInvocationHandler(ref, containerHandler, path);
    }

    @SuppressWarnings("unchecked")
    private EntitySetInvocationHandler(
            final Class<?> ref,
            final EntityContainerInvocationHandler containerHandler,
            final String path) {
        super(containerHandler.getClient(), containerHandler);

        final Annotation annotation = ref.getAnnotation(EntitySet.class);
        checkAnnotation(ref, annotation);

        this.entitySetName = ((EntitySet) annotation).name();
        this.entitySetPath = this.createPath = path;

        final Type[] abstractEntitySetParams =
                ((ParameterizedType) ref.getGenericInterfaces()[0]).getActualTypeArguments();

        this.typeRef = (Class<T>) abstractEntitySetParams[0];
        if (typeRef.getAnnotation(EntityType.class) == null) {
            throw new IllegalArgumentException("Invalid entity '" + typeRef.getSimpleName() + "'");
        }
        this.collTypeRef = (Class<EC>) abstractEntitySetParams[2];

        this.uri = buildURI(containerHandler, path);
    }

    @SuppressWarnings("unchecked")
    private EntitySetInvocationHandler(
            final Class<?> ref,
            final EntityContainerInvocationHandler containerHandler) throws UnsupportedEncodingException {

        super(containerHandler.getClient(), containerHandler);

        final Annotation annotation = ref.getAnnotation(EntitySet.class);
        checkAnnotation(ref, annotation);

        this.entitySetName = ((EntitySet) annotation).name();
        this.entitySetPath = ((EntitySet) annotation).path();
        this.createPath = ((EntitySet) annotation).createPath();

        final Type[] abstractEntitySetParams =
                ((ParameterizedType) ref.getGenericInterfaces()[0]).getActualTypeArguments();

        this.typeRef = (Class<T>) abstractEntitySetParams[0];
        if (typeRef.getAnnotation(EntityType.class) == null) {
            throw new IllegalArgumentException("Invalid entity '" + typeRef.getSimpleName() + "'");
        }
        this.collTypeRef = (Class<EC>) abstractEntitySetParams[2];

        this.uri = buildURI(containerHandler, URLEncoder.encode(entitySetPath, "utf-8"));
    }

    /**
     * Builds URI for current entity set
     *
     * @param containerHandler Container handler.
     * @param path path to entity set relative to service root.
     * @return Uri was built.
     */
    private URI buildURI(final EntityContainerInvocationHandler containerHandler, String path) {
        final URIBuilder uriBuilder = client.getURIBuilder(containerHandler.getFactory().getServiceRoot());

        if (!containerHandler.isDefaultEntityContainer()) {
            uriBuilder.appendStructuralSegment(containerHandler.getEntityContainerName()).appendStructuralSegment(".");
        }

        uriBuilder.appendEntitySetSegment(path);
        return uriBuilder.build();
    }

    private void checkAnnotation(final Class<?> ref, final Annotation annotation) {
        if (!(annotation instanceof EntitySet)) {
            throw new IllegalArgumentException("Return type " + ref.getName()
                    + " is not annotated as @" + EntitySet.class.getSimpleName());
        }
    }

    Class<T> getTypeRef() {
        return typeRef;
    }

    Class<EC> getCollTypeRef() {
        return collTypeRef;
    }

    String getEntitySetName() {
        return entitySetName;
    }

    URI getUri() {
        return uri;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        if (isSelfMethod(method, args)) {
            return invokeSelfMethod(method, args);
        } else if (method.getName().startsWith("new")) {
            if (method.getName().endsWith("Collection") && ArrayUtils.isEmpty(args)) {
                return newEntityCollection(method.getReturnType());
            } else if (ArrayUtils.isEmpty(args) || (!ArrayUtils.isEmpty(args) && args.length <= 1)) {
                return args != null && args.length > 0 ?
                        newEntity(method.getReturnType(), (String) args[0]) :
                        newEntity(method.getReturnType(), entitySetName);
            } else {
                throw new UnsupportedOperationException("Method not found: " + method);
            }
        }

        throw new UnsupportedOperationException("Method not found: " + method);
    }


    @SuppressWarnings("unchecked")
    private <NE> NE newEntity(final Class<NE> reference, String entitySetName) throws ClassNotFoundException {
        LOG.debug("New entity in {}", this.entitySetName);
        final ODataEntity entity = ODataObjectFactory.newEntity(
                containerHandler.getSchemaName() + "." + ClassUtils.getEntityTypeName(reference));

        final EntityTypeInvocationHandler handler = EntityTypeInvocationHandler.getInstance(
                entity, containerHandler.getEntityContainerName(), String.format(createPath, entitySetName), reference, containerHandler);
        EntityContainerFactory.getContext().entityContext().attachNew(handler);

        return (NE) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class<?>[] {reference},
                handler);
    }

    @SuppressWarnings("unchecked")
    private <NEC> NEC newEntityCollection(final Class<NEC> reference) {
        LOG.debug("New collection in {}", this.entitySetName);
        return (NEC) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class<?>[] {reference},
                new EntityCollectionInvocationHandler<T>(
                containerHandler, new ArrayList<T>(), typeRef, containerHandler.getEntityContainerName()));
    }

    @Override
    public Long count() {
        final ODataValueRequest req = client.getRetrieveRequestFactory().getValueRequest(
                client.getURIBuilder(uri.toASCIIString()).appendCountSegment().build());
        req.setFormat(ODataValueFormat.TEXT);
        return Long.valueOf(req.execute().getBody().asPrimitive().toString());
    }
    
    @Override
    public ListenableFuture<Long> countAsync() {
        return containerHandler.getExecutorService().submit(new Callable<Long>() {
            @Override
            public Long call() {
                return count();
            }
        });
    }

    @Override
    public Boolean exists(final KEY key) throws IllegalArgumentException {
        boolean result = false;

        try {
            result = get(key) != null;
        } catch (Exception e) {
            LOG.error("Could not check existence of {}({})", entitySetName, key, e);
        }

        return result;
    }
    
    @Override
    public ListenableFuture<Boolean> existsAsync(final KEY key) {
        return containerHandler.getExecutorService().submit(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return exists(key);
            }
        });
    }

    private LinkedHashMap<String, Object> getCompoundKey(final Object key) {
        final Set<CompoundKeyElementWrapper> elements = new TreeSet<CompoundKeyElementWrapper>();

        for (Method method : key.getClass().getMethods()) {
            final Annotation annotation = method.getAnnotation(CompoundKeyElement.class);
            if (annotation instanceof CompoundKeyElement) {
                elements.add(new CompoundKeyElementWrapper(
                        ((CompoundKeyElement) annotation).name(), method, ((CompoundKeyElement) annotation).position()));
            }
        }

        final LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();

        for (CompoundKeyElementWrapper element : elements) {
            try {
                map.put(element.getName(), element.getMethod().invoke(key));
            } catch (Exception e) {
                LOG.warn("Error retrieving compound key element '{}' value", element.getName(), e);
            }
        }

        return map;
    }

    @Override
    public T get(KEY key) throws IllegalArgumentException {
        return get(key, typeRef);
    }
    
    @Override
    public ListenableFuture<T> getAsync(final KEY key) {
        return containerHandler.getExecutorService().submit(new Callable<T>() {
            @Override
            public T call() {
                return get(key);
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S extends T> S get(final KEY key, final Class<S> typeRef) throws IllegalArgumentException {
        if (key == null) {
            throw new IllegalArgumentException("Null key");
        }

        final EntityUUID uuid = new EntityUUID(ClassUtils.getNamespace(typeRef), containerHandler.getEntityContainerName(), entitySetName,
                ClassUtils.getNamespace(typeRef) + "." + ClassUtils.getEntityTypeName(typeRef), key);

        LOG.debug("Ask for '{}({})'", typeRef.getSimpleName(), key);

        EntityTypeInvocationHandler handler = EntityContainerFactory.getContext().entityContext().getEntity(uuid);

        if (handler == null) {
            // not yet attached: search against the service
            try {
                LOG.debug("Search for '{}({})' into the service", typeRef.getSimpleName(), key);
                final URIBuilder uriBuilder = client.getURIBuilder(uri.toASCIIString());

                if (key.getClass().getAnnotation(CompoundKey.class) == null) {
                    LOG.debug("Append key segment '{}'", key);
                    uriBuilder.appendKeySegment(key);
                } else {
                    LOG.debug("Append compound key segment '{}'", key);
                    uriBuilder.appendKeySegment(getCompoundKey(key));
                }

                LOG.debug("Execute query '{}'", uriBuilder.toString());

                final ODataRetrieveResponse<ODataEntity> res =
                        client.getRetrieveRequestFactory().getEntityRequest(uriBuilder.build()).execute();

                handler = EntityTypeInvocationHandler.getInstance(res.getBody(), containerHandler.getEntityContainerName(), entitySetName,
                        typeRef, containerHandler);
                handler.setETag(res.getEtag());
            } catch (ODataClientErrorException e) {
                // return null only if server responded with 404 status code;
                //all other exceptions will be thrown for further handling
                if (e.getStatusLine().getStatusCode() == 404) {
                    LOG.info("Entity '" + uuid + "' not found", e);
                } else {
                    throw new RuntimeException(e);
                }
            }
        } else if (isDeleted(handler)) {
            // object deleted
            LOG.debug("Object '{}({})' has been delete", typeRef.getSimpleName(), uuid);
            handler = null;
        }

        return handler == null ? null : (S) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class<?>[] {typeRef},
                handler);
    }

    @Override
    public <S extends T> ListenableFuture<S> getAsync(final KEY key, final Class<S> reference) {
        return containerHandler.getExecutorService().submit(new Callable<S>() {
            @Override
            public S call() {
                return get(key, reference);
            }
        });
    }
    
    @SuppressWarnings("unchecked")
    public <S extends T> Map.Entry<List<S>, URI> fetchPartialEntitySet(final URI uri, final Class<S> typeRef) {
        final ODataRetrieveResponse<ODataEntitySet> res =
                client.getRetrieveRequestFactory().getEntitySetRequest(uri).execute();

        final ODataEntitySet entitySet = res.getBody();

        final List<S> items = new ArrayList<S>(entitySet.getEntities().size());
        for (ODataEntity entity : entitySet.getEntities()) {
            final EntityTypeInvocationHandler handler = EntityTypeInvocationHandler.getInstance(entity, this, typeRef);

            final EntityTypeInvocationHandler handlerInTheContext =
                    EntityContainerFactory.getContext().entityContext().getEntity(handler.getUUID());

            items.add((S) Proxy.newProxyInstance(
                    Thread.currentThread().getContextClassLoader(),
                    new Class<?>[] {typeRef},
                    handlerInTheContext == null ? handler : handlerInTheContext));
        }

        return new AbstractMap.SimpleEntry<List<S>, URI>(items, entitySet.getNext());
    }

    @SuppressWarnings("unchecked")
    public <S extends T, SEC extends AbstractEntityCollection<S>> SEC fetchWholeEntitySet(
            final URI entitySetURI, final Class<S> typeRef, final Class<SEC> collTypeRef) {

        final List<S> items = new ArrayList<S>();

        URI nextURI = entitySetURI;
        while (nextURI != null) {
            final Map.Entry<List<S>, URI> entitySet = fetchPartialEntitySet(nextURI, typeRef);
            nextURI = entitySet.getValue();
            items.addAll(entitySet.getKey());
        }

        return (SEC) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class<?>[] {collTypeRef},
                new EntityCollectionInvocationHandler<S>(
                containerHandler, items, typeRef, containerHandler.getEntityContainerName(), entitySetURI));
    }

    @Override
    public EC getAll() {
        return getAll(collTypeRef);
    }
    
    @Override
    public ListenableFuture<EC> getAllAsync() {
        return containerHandler.getExecutorService().submit(new Callable<EC>() {
            @Override
            public EC call() {
                return getAll();
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S extends T, SEC extends AbstractEntityCollection<S>> SEC getAll(final Class<SEC> collTypeRef) {
        final Class<S> typeRef = (Class<S>) ClassUtils.extractTypeArg(collTypeRef);

        // TODO append entity type in v3
        final URI entitySetURI = client.getURIBuilder(uri.toASCIIString()).build();

        return fetchWholeEntitySet(entitySetURI, typeRef, collTypeRef);
    }
    
    @Override
    public <S extends T, SEC extends AbstractEntityCollection<S>> ListenableFuture<SEC> getAllAsync(final Class<SEC> reference) {
        return containerHandler.getExecutorService().submit(new Callable<SEC>() {
            @Override
            public SEC call() {
                return getAll(reference);
            }
        });
    }

    @Override
    public Query<T, EC> createQuery() {
        return new QueryImpl<T, EC>(this.client, this.collTypeRef, this.uri, this);
    }

    @Override
    public <S extends T, SEC extends AbstractEntityCollection<S>> Query<S, SEC> createQuery(
            final Class<SEC> reference) {

        return new QueryImpl<S, SEC>(this.client, reference, this.uri, this);
    }

    @Override
    public void delete(final KEY key) throws IllegalArgumentException {
        final EntityContext entityContext = EntityContainerFactory.getContext().entityContext();

        EntityTypeInvocationHandler entity = entityContext.getEntity(new EntityUUID(
                ClassUtils.getNamespace(typeRef),
                containerHandler.getEntityContainerName(),
                entitySetName,
                ClassUtils.getNamespace(typeRef) + "." + ClassUtils.getEntityTypeName(typeRef),
                key));

        if (entity == null) {
            // search for entity
            final T searched = get(key);
            entity = (EntityTypeInvocationHandler) Proxy.getInvocationHandler(searched);
            entityContext.attach(entity, AttachedEntityStatus.DELETED);
        } else {
            entityContext.setStatus(entity, AttachedEntityStatus.DELETED);
        }
    }

    @Override
    public <S extends T> void delete(final Iterable<S> entities) {
        final EntityContext entityContext = EntityContainerFactory.getContext().entityContext();

        for (T en : entities) {
            final EntityTypeInvocationHandler entity = (EntityTypeInvocationHandler) Proxy.getInvocationHandler(en);
            if (entityContext.isAttached(entity)) {
                entityContext.setStatus(entity, AttachedEntityStatus.DELETED);
            } else {
                entityContext.attach(entity, AttachedEntityStatus.DELETED);
            }
        }
    }

    private boolean isDeleted(final EntityTypeInvocationHandler handler) {
        return EntityContainerFactory.getContext().entityContext().getStatus(handler) == AttachedEntityStatus.DELETED;
    }

    @Override
    public Iterator<T> iterator() {
        if (setAsCollection != null) {
            return setAsCollection.iterator();
        } else {
            return mEmptyIterator;
        }
    }

    @Override
    public boolean add(T e) {
        if (setAsCollection == null) {
            try {
                fetch();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return setAsCollection.add(e);
    }

    @Override
    public void fetch() {
        try {
            setAsCollection = new EntityCollectionInvocationHandler<T>(
                    containerHandler,
                    getAll(),
                    typeRef,
                    containerHandler.getEntityContainerName());
        } catch (Exception e) {/* keep it null? */}
    }
    
    @Override
    public ListenableFuture<Void> fetchAsync() {
        return containerHandler.getExecutorService().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                fetch();
                return ClassUtils.returnVoid();
            }
        });
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        if (setAsCollection == null) {
            try {
                fetch();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return setAsCollection.addAll(c);
    }

    @Override
    public void clear() {
        if (setAsCollection == null) {
            try {
                fetch();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        setAsCollection.clear();
    }

    @Override
    public boolean contains(Object o) {
        if (setAsCollection == null) {
            try {
                fetch();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        return setAsCollection.contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        if (setAsCollection == null) {
            try {
                fetch();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        return setAsCollection.containsAll(c);
    }

    @Override
    public boolean isEmpty() {
        if (setAsCollection == null) {
            try {
                fetch();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        return setAsCollection.isEmpty();
    }

    @Override
    public boolean remove(Object o) {
        if (setAsCollection == null) {
            try {
                fetch();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        return setAsCollection.remove(o);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if (setAsCollection == null) {
            try {
                fetch();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        return setAsCollection.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        if (setAsCollection == null) {
            try {
                fetch();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        return setAsCollection.retainAll(c);
    }

    @Override
    public int size() {
        if (setAsCollection == null) {
            try {
                fetch();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        return setAsCollection.size();
    }

    @Override
    public Object[] toArray() {
        if (setAsCollection == null) {
            try {
                fetch();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        return setAsCollection.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        if (setAsCollection == null) {
            try {
                fetch();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        return setAsCollection.toArray(a);
    }
}
