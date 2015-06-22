package org.glassfish.jersey.internal;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Descriptor;
import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorState;
import org.glassfish.hk2.api.Unqualified;
import org.glassfish.hk2.utilities.AbstractActiveDescriptor;
import org.glassfish.jersey.spi.ExtendedExceptionMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Unit test of {@link ExceptionMapperFactory}.
 */
public class ExceptionMapperFactoryTest {

    private IllegalArgumentExceptionMapper illegalArgumentExceptionMapper;
    private IllegalStateExceptionMapper illegalStateExceptionMapper;
    private RuntimeExceptionMapper runtimeExceptionMapper;

    private ServiceHandle<?> iaeServiceHandle;
    private ServiceHandle<?> iseServiceHandle;
    private ServiceHandle<?> rteServiceHandle;

    @Before
    public void setup() {
        ActiveDescriptorStub<IllegalArgumentExceptionMapper> iaeDescriptor =
                new ActiveDescriptorStub<IllegalArgumentExceptionMapper>(new IllegalArgumentExceptionMapper());
        ActiveDescriptorStub<IllegalStateExceptionMapper> iseDescriptor =
                new ActiveDescriptorStub<IllegalStateExceptionMapper>(new IllegalStateExceptionMapper());
        ActiveDescriptorStub<RuntimeExceptionMapper> rteDescriptor =
                new ActiveDescriptorStub<RuntimeExceptionMapper>(new RuntimeExceptionMapper());

        illegalArgumentExceptionMapper = new IllegalArgumentExceptionMapper();
        illegalStateExceptionMapper = new IllegalStateExceptionMapper();
        runtimeExceptionMapper = new RuntimeExceptionMapper();

        iaeServiceHandle = new ServiceHandleStub<IllegalArgumentExceptionMapper>(illegalArgumentExceptionMapper,
                true, iaeDescriptor);
        iseServiceHandle = new ServiceHandleStub<IllegalStateExceptionMapper>(illegalStateExceptionMapper, true, iseDescriptor);
        rteServiceHandle = new ServiceHandleStub<RuntimeExceptionMapper>(runtimeExceptionMapper, true, rteDescriptor);
    }

    /**
     * Test spec:
     * <p/>
     * setup:<br/>
     * - have two extended exception mappers, order matters<br/>
     * - both using the same generic type (RuntimeException)<br/>
     * - first mapper return isMappable true only to IllegalArgumentException<br/>
     * - second mapper return isMappable true only to IllegalStateException<br/>
     * <br/>
     * when:<br/>
     * - {@link ExceptionMapperFactory#findMapping(Throwable)} with IllegalArgumentException instance<br/>
     * <br/>
     * then:<br/>
     * - exception mapper factory returns IllegalArgumentExceptionMapper<br/>
     * <p/>
     * why:<br/>
     * - IllegalArgumentException has the same distance (1) for both exception mappers generic type (RuntimeException),
     * but IllegalArgumentException's isMappable return true, so it is the winner
     *
     * @throws Exception unexpected - if anything goes wrong, the test fails
     */
    @Test
    public void testFindMappingExtendedExceptions() throws Exception {
        List<ServiceHandle<?>> serviceHandles = Arrays.asList(iaeServiceHandle, iseServiceHandle);
        ExceptionMapperFactory mapperFactory = new ExceptionMapperFactory(new ServiceLocatorStub(serviceHandles));

        ExceptionMapper<IllegalArgumentException> mapper = mapperFactory.findMapping(new IllegalArgumentException());
        Assert.assertEquals("IllegalArgumentExceptionMapper should be returned", illegalArgumentExceptionMapper, mapper);
    }

    /**
     * Test spec:
     * <p/>
     * setup:<br/>
     * - have 3 exception mappers, order matters<br/>
     * - first is *not* extended mapper typed to RuntimeException
     * - second and third are extended mappers type to RuntimeException
     * <br/>
     * when:<br/>
     * - {@link ExceptionMapperFactory#findMapping(Throwable)} invoked with RuntimeException instance<br/>
     * then: <br/>
     * - exception mapper factory returns RuntimeExceptionMapper<br/>
     * <p/>
     * why:<br/>
     * - RuntimeException mapper has distance 0 for RuntimeException, it is not extended mapper, so it will be chosen
     * immediately, cause there is no better option possible
     *
     * @throws Exception unexpected - if anything goes wrong, the test fails
     */
    @Test
    public void testFindMapping() throws Exception {
        List<ServiceHandle<?>> serviceHandles = Arrays.asList(rteServiceHandle, iaeServiceHandle, iseServiceHandle);
        ExceptionMapperFactory mapperFactory = new ExceptionMapperFactory(new ServiceLocatorStub(serviceHandles));

        ExceptionMapper<RuntimeException> mapper = mapperFactory.findMapping(new RuntimeException());
        Assert.assertEquals("RuntimeExceptionMapper should be returned", runtimeExceptionMapper, mapper);
    }

    /**
     * Test spec: <br/>
     * <p/>
     * setup:<br/>
     * - have 2 extended mappers, order matters<br/>
     * - first mapper return isMappable true only to IllegalArgumentException<br/>
     * - second mapper return isMappable true only to IllegalStateException<br/>
     * <br/>
     * when:<br/>
     * - {@link ExceptionMapperFactory#find(Class)} invoked with IllegalArgumentException.class<br/>
     * then:<br/>
     * - exception mapper factory returns IllegalArgumentExceptionMapper<br/>
     * <p/>
     * why:<br/>
     * - both exception mappers have distance 1 to IllegalArgumentException, we don't have instance of the
     * IllegalArgumentException, so the isMappable check is not used and both are accepted, the later accepted is
     * the winner
     *
     * @throws Exception unexpected - if anything goes wrong, the test fails
     */
    @Test
    public void testFindExtendedExceptions() throws Exception {
        List<ServiceHandle<?>> serviceHandles = Arrays.asList(iaeServiceHandle, iseServiceHandle);
        ExceptionMapperFactory mapperFactory = new ExceptionMapperFactory(new ServiceLocatorStub(serviceHandles));

        ExceptionMapper<IllegalArgumentException> mapper = mapperFactory.find(IllegalArgumentException.class);
        Assert.assertEquals("IllegalStateExceptionMapper should be returned", illegalStateExceptionMapper, mapper);
    }


    /**
     * Extended Exception Mapper which has RuntimeException as generic type and isMappable returns true if the
     * exception is instance of IllegalArgumentException.
     */
    private static class IllegalArgumentExceptionMapper implements ExtendedExceptionMapper<RuntimeException> {

        @Override
        public boolean isMappable(RuntimeException exception) {
            return exception instanceof IllegalArgumentException;
        }

        @Override
        public Response toResponse(RuntimeException exception) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .build();
        }

    }

    /**
     * Extended Exception Mapper which has RuntimeException as generic type and isMappable returns true if the
     * exception is instance of IllegalStateException.
     */
    private static class IllegalStateExceptionMapper implements ExtendedExceptionMapper<RuntimeException> {

        @Override
        public boolean isMappable(RuntimeException exception) {
            return exception instanceof IllegalStateException;
        }

        @Override
        public Response toResponse(RuntimeException exception) {
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .build();
        }

    }

    /**
     * Exception Mapper which has RuntimeException as generic type.
     */
    private static class RuntimeExceptionMapper implements ExceptionMapper<RuntimeException> {

        @Override
        public Response toResponse(RuntimeException exception) {
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .build();
        }

    }


    /**
     * Stub implementation -- reason for this is to control the order of list of services in ServiceLocator.
     *
     * @param <T>
     */
    private static class ActiveDescriptorStub<T> extends AbstractActiveDescriptor<T> {

        private final T impl;

        public ActiveDescriptorStub(T impl) {
            this.impl = impl;
        }


        @Override
        public Class<?> getImplementationClass() {
            return impl.getClass();
        }

        @Override
        public T create(ServiceHandle<?> serviceHandle) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            ActiveDescriptorStub<?> that = (ActiveDescriptorStub<?>) o;
            return Objects.equals(impl, that.impl);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), impl);
        }
    }

    /**
     * Stub implementation -- reason for this is to control the order of list of services in ServiceLocator.
     *
     * @param <T>
     */
    private static class ServiceHandleStub<T> implements ServiceHandle<T> {

        private final T service;
        private final boolean active;
        private final ActiveDescriptor<T> descriptor;

        private Object serviceData;

        public ServiceHandleStub(T service, boolean active, ActiveDescriptor<T> descriptor) {
            this.service = service;
            this.active = active;
            this.descriptor = descriptor;
        }

        @Override
        public T getService() {
            return service;
        }

        @Override
        public ActiveDescriptor<T> getActiveDescriptor() {
            return descriptor;
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public void destroy() {
            // no op
        }

        @Override
        public void setServiceData(Object serviceData) {
            this.serviceData = serviceData;
        }

        @Override
        public Object getServiceData() {
            return serviceData;
        }

    }

    /**
     * Stub implementation -- reason for this is to control the order of list of services.
     */
    private static class ServiceLocatorStub implements ServiceLocator {

        private final List<ServiceHandle<?>> serviceHandles;

        private ServiceLocatorStub(List<ServiceHandle<?>> serviceHandles) {
            this.serviceHandles = serviceHandles;
        }

        @Override
        public <T> T getService(Class<T> aClass, Annotation... annotations) throws MultiException {
            return null;
        }

        @Override
        public <T> T getService(Type type, Annotation... annotations) throws MultiException {
            return null;
        }

        @Override
        public <T> T getService(Class<T> aClass, String s, Annotation... annotations) throws MultiException {
            return null;
        }

        @Override
        public <T> T getService(Type type, String s, Annotation... annotations) throws MultiException {
            return null;
        }

        @Override
        public <T> List<T> getAllServices(Class<T> aClass, Annotation... annotations) throws MultiException {
            return null;
        }

        @Override
        public <T> List<T> getAllServices(Type type, Annotation... annotations) throws MultiException {
            return null;
        }

        @Override
        public <T> List<T> getAllServices(Annotation annotation, Annotation... annotations) throws MultiException {
            return null;
        }

        @Override
        public List<?> getAllServices(Filter filter) throws MultiException {
            return null;
        }

        @Override
        public <T> ServiceHandle<T> getServiceHandle(Class<T> aClass, Annotation... annotations) throws MultiException {
            return null;
        }

        @Override
        public <T> ServiceHandle<T> getServiceHandle(Type type, Annotation... annotations) throws MultiException {
            return null;
        }

        @Override
        public <T> ServiceHandle<T> getServiceHandle(Class<T> aClass, String s, Annotation... annotations) throws MultiException {
            return null;
        }

        @Override
        public <T> ServiceHandle<T> getServiceHandle(Type type, String s, Annotation... annotations) throws MultiException {
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> List<ServiceHandle<T>> getAllServiceHandles(Class<T> aClass, Annotation... annotations) throws MultiException {
            if (annotations != null && annotations.length == 0) {
                List<ServiceHandle<T>> handles = new ArrayList<ServiceHandle<T>>();
                for (ServiceHandle serviceHandle : serviceHandles) {
                    handles.add((ServiceHandle<T>) serviceHandle);
                }
                return handles;
            }
            return Collections.emptyList();
        }

        @Override
        public List<ServiceHandle<?>> getAllServiceHandles(Type type, Annotation... annotations) throws MultiException {
            return null;
        }

        @Override
        public List<ServiceHandle<?>> getAllServiceHandles(Annotation annotation,
                                                           Annotation... annotations) throws MultiException {
            return null;
        }

        @Override
        public List<ServiceHandle<?>> getAllServiceHandles(Filter filter) throws MultiException {
            return null;
        }

        @Override
        public List<ActiveDescriptor<?>> getDescriptors(Filter filter) {
            return null;
        }

        @Override
        public ActiveDescriptor<?> getBestDescriptor(Filter filter) {
            return null;
        }

        @Override
        public ActiveDescriptor<?> reifyDescriptor(Descriptor descriptor,
                                                   Injectee injectee) throws MultiException {
            return null;
        }

        @Override
        public ActiveDescriptor<?> reifyDescriptor(Descriptor descriptor)
                throws MultiException {
            return null;
        }

        @Override
        public ActiveDescriptor<?> getInjecteeDescriptor(Injectee injectee)
                throws MultiException {
            return null;
        }

        @Override
        public <T> ServiceHandle<T> getServiceHandle(ActiveDescriptor<T> activeDescriptor,
                                                     Injectee injectee) throws MultiException {
            return null;
        }

        @Override
        public <T> ServiceHandle<T> getServiceHandle(ActiveDescriptor<T> activeDescriptor)
                throws MultiException {
            return null;
        }

        @Override
        public <T> T getService(ActiveDescriptor<T> activeDescriptor,
                                ServiceHandle<?> serviceHandle) throws MultiException {
            return null;
        }

        @Override
        public <T> T getService(ActiveDescriptor<T> activeDescriptor,
                                ServiceHandle<?> serviceHandle, Injectee injectee) throws MultiException {
            return null;
        }

        @Override
        public String getDefaultClassAnalyzerName() {
            return null;
        }

        @Override
        public void setDefaultClassAnalyzerName(String s) {

        }

        @Override
        public Unqualified getDefaultUnqualified() {
            return null;
        }

        @Override
        public void setDefaultUnqualified(Unqualified unqualified) {

        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public long getLocatorId() {
            return 0;
        }

        @Override
        public ServiceLocator getParent() {
            return null;
        }

        @Override
        public void shutdown() {

        }

        @Override
        public ServiceLocatorState getState() {
            return null;
        }

        @Override
        public boolean getNeutralContextClassLoader() {
            return false;
        }

        @Override
        public void setNeutralContextClassLoader(boolean b) {

        }

        @Override
        public <T> T create(Class<T> aClass) {
            return null;
        }

        @Override
        public <T> T create(Class<T> aClass, String s) {
            return null;
        }

        @Override
        public void inject(Object o) {

        }

        @Override
        public void inject(Object o, String s) {

        }

        @Override
        public void postConstruct(Object o) {

        }

        @Override
        public void postConstruct(Object o, String s) {

        }

        @Override
        public void preDestroy(Object o) {

        }

        @Override
        public void preDestroy(Object o, String s) {

        }

        @Override
        public <U> U createAndInitialize(Class<U> aClass) {
            return null;
        }

        @Override
        public <U> U createAndInitialize(Class<U> aClass, String s) {
            return null;
        }
    }
}
