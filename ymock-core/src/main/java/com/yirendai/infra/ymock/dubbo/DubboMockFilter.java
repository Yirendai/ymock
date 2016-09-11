package com.yirendai.infra.ymock.dubbo;

import java.lang.reflect.InvocationTargetException;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcResult;

@Activate(group = {Constants.PROVIDER, Constants.CONSUMER})
public class DubboMockFilter implements Filter {

  public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
    if (!DubboMockManager.isMock()) {
      return invoker.invoke(invocation);
    }

    RpcContext context = RpcContext.getContext();
    boolean isConsumerSide = context.isConsumerSide();

    if (!isConsumerSide) {
      return invoker.invoke(invocation);
    }

    URL url = context.getUrl();
    String interfaceName = url.getServiceInterface();

    if (!DubboMockManager.isMock(interfaceName)) {
      return invoker.invoke(invocation);
    }

    String methodName = context.getMethodName();
    Class<?>[] paraTypes = context.getParameterTypes();
    Object[] paraObjects = context.getArguments();

    Object value = null;
    try {
      value = DubboMock.invoke(interfaceName, methodName, paraTypes, paraObjects);
    } catch (IllegalAccessException ex) {
      ex.printStackTrace();
      throw new RpcException("dubbo invoke error:" + interfaceName + "," + methodName);
    } catch (IllegalArgumentException ex) {
      ex.printStackTrace();
      throw new RpcException("dubbo invoke error:" + interfaceName + "," + methodName);
    } catch (InvocationTargetException ex) {
      ex.printStackTrace();
      throw new RpcException("dubbo invoke error:" + interfaceName + "," + methodName);
    }

    RpcResult rpcResult = new RpcResult();
    rpcResult.setValue(value);
    return rpcResult;
  }

}
