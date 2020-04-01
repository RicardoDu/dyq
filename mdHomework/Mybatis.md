##### 一、Mybatis动态sql是做什么的？都有哪些动态sql？简述一下动态sql的执行原理？

-  mybatis的动态sql： 

    	​根据实体参数不同的取值来拼接出不同的sql语句进行查询。

- 动态sql标签：

  	​	trim, where,set,foreach,if,choose,when,otherwise,bind

- 动态sql的执行原理：

   1. 在mybatis框架中，SqlSourceBuilder将xml里面的sql语句解析成为一个一个由SqlNode对象描述的节点。每一个动态sql都是由多个sqlNode节点构成，其根节点为MixedSqlNode 。
   2. 每当创建一个SqlSource对象时首先会创建一个 XMLScriptBuilder 对象，此时会调用initNodeHandlerMap()方法将动态sql标签初始化到nodeHandlerMap<String,NodeHandler>()中。
   3. 然后通过XMLScriptBuilder对象来调用parseScriptNode()方法
   4. 将 SQL 解析成 SqlSource 对象，在解析过程中parseDynamicTags()方法将sql解析为MixedSqlNode 对象，并说明其是否为动态，在创建MixedSqlNode 的时候会对各个子节点进行分析：
      -  不包含动态sql标签的如果是动态sql则创建为TextSqlNode，静态sql则StaticTextSqlNode
      -  包含动态sql标签的则为各自的对应node，eg(where标签->WhereSqlNode)
   5. 通过MixedSqlNode生成DynamicSqlSource（RawSqlSource）动态（静态）SQL对象
   6. 在初始化结束后当通过executor调用query()方法时，依据configuration和prepareStatement参数执行根节点apply方法，一次执行各个子节点的apply方法，最后通过GenericTokenParser将sql拼接为？形式的jdbc语句

##### 二、Mybatis是否支持延迟加载？如果支持，它的实现原理是什么？

- Mybatis仅支持association关联对象（一对一）和collection关联集合对象（一对多）的延迟加载。

- 原理：使用CGLIB创建目标对象的代理对象，当调用目标方法时，会被拦截器invoke方法拦截处理，如果发现调用的关联对象为空，则在此时先发送之前保存好的查询关联对象的sql，获取到关联对象的值，再将关联对象的值存储到主体对象中再进行之前的操作。

##### 三、Mybatis都有哪些Executor执行器？它们之间的区别是什么？

- SimpleExecutor：每执行一次update或select，就开启一个Statement对象，用完立刻关闭Statement对象。

- ReuseExecutor：执行update或select，以sql作为key查找Statement对象，存在就使用，不存在就创建，用完后，不关闭Statement对象，而是放置于Map内，供下一次使用。简言之，就是重复使用Statement对象。

- BatchExecutor：执行update（没有select，JDBC批处理不支持select），将所有sql都添加到批处理中（addBatch()），等待统一执行（executeBatch()），它缓存了多个Statement对象，每个Statement对象都是addBatch()完毕后，等待逐一执行executeBatch()批处理。与JDBC批处理相同。

##### 四、简述下Mybatis的一级、二级缓存（分别从存储结构、范围、失效场景。三个方面来作答）？

- 一级缓存：查询的时候首先去一级缓存中查询，没有则查询数据库并将结果对象存入到一级缓存中，如果进行了事务提交则刷新一级缓存（默认开启）。
  1. 存储结构：sqlsession中的HashMap：
     - key: 由statementid（namespace.id的声明）,params（参数）,boundSql（Mybatis底层对象，封装着要执行的sql语句）,rowBounds（分页对象）组成
     - value:查询回的结果对象
  2. 范围：同一个SqlSession对象
  3. 失效场景：
     - 只要执行commit方法，那么就会直接将SqlSession对象全部清空掉。
     - sqlsession.clearCache()：手动清空一级缓存
     - sqlsession.close()
- 二级缓存：和一级缓存类似但是二级缓存是基于mapper的且缓存的是数据而不是对象（默认关闭：需要手动配置生效）
  1. 存储结构：通过实现cache接口在mapper/redis等分布式缓存框架 中存储的Hash结构
  2. 范围：同一个mapper.xml,一个namespace对应一个二级缓存
  3. 失效场景：执行事务操作

##### 五、简述Mybatis的插件运行原理，以及如何编写一个插件？

- Mybatis的插件运行原理:

  ​	Mybatis仅可以编写针对ParameterHandler、ResultSetHandler、StatementHandler、Executor这4种接口的插件，Mybatis使用JDK的动态代理，为需要拦截的接口生成代理对象以实现接口方法拦截功能，每当执行这4种接口对象的方法时，就会进入拦截方法，具体就是InvocationHandler的invoke()方法，当然，只会拦截那些你指定需要拦截的方法。

- 如何编写一个插件：

  ​	实现Mybatis的Interceptor接口并复写intercept()方法，然后再给插件编写注解，指定要拦截哪一个接口的哪些方法并在配置文件中对插件进行配置。





