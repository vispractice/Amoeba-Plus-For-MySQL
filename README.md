`Amoeba Plus For MySQL`是基于`Amoeba For MySQL`项目的一个改进版本。在保留基本的透明分库分表和读写分离特性的基础上，增加了如下特性：

- 多用户  
  用户名作为路由的一个依据，这样可以为不同的用户配置不同的路由规则。

        -- user.xml
        -- 配置用户
        <user name="root" passwd="yxtech" />
        <user name="other" passwd="yxtech" />

        -- rule.xml
        -- 表规则和用户关联
        <tableRule name="*" schema="test" users="root" defaultPools="writePool" />
        <tableRule name="*" schema="other" users="other" defaultPools="writePool" />

- 事务  
  支持单库事务。分布式事务是采用MySQL原有的XA机制，需主动声明开启XA事务。声明开启XA是通过SQL Hint的方式进行的：

        set autocommit = 0
        /* set isXA = 1 */
        .....
        commit

- 全局序列(需要自己实现ID生成器)  
  在做水平切分时，需要保证同一个表的不同分片的主键全局唯一性。做法有很多，一种常见的做法就是通过全局序列。`Amoeba Plus`提供了全局序列的方式，全局序列的生成方式需要自己实现。`Amoeba Plus`默认提供了一个内存全局序列生成器。

        -- 创建全局序列
        CREATE SEQUENCE customers_seq;
        CREATE SEQUENCE customers_seq START WITH 1000;
        CREATE SEQUENCE customers_seq START WITH 1000 INCREMENT BY 1;

        -- 删除全局序列
        DROP SEQUENCE customers_seq;

        -- 使用全局序列
        SELECT customers_seq.currval FROM DUAL;
        SELECT customers_seq.nextval FROM DUAL;
        INSERT foo (id, value) values (customers_seq.nextval, 0);

- 简单的SQL Hint  
  通过SQL Hint，可以人为干预`Amoeba Plus`的路由结果。最典型的，在读写分离时，为了在写完之后马上读到最新的结果，就需要将读语句强制发往master库，从而避免复制延迟可能导致的数据不一致。

        inster into ...
        /* isRead=false */ select * from ...  # 强制发往写库

   有时候甚至需要人工指定路由目标：

        /* pools="pool#1, pool#2" */ select * from ... # 指定发往pool#1和pool#2

- 一些语句的解析增强  
  当我们用`TPCC-MySQL`或`sysbench`压测`Amoeba`时，会出现由于语句解析而导致路由失败的情况，针对这些情况，我们做了SQL解析上的一些增强。另外增加DDL语句的识别。

- 多种配置数据源的适配  
  `Amoeba`默认的配置是存储在XML文件中，后面我们将配置的加载从`Amoeba`的核心中抽取出来，作为独立的组件。这样可以方便的支持不同的配置数据源。

## 如何运行 ##

- 安装JDK 1.7及以上，
- 准备测试数据库
  默认的配置是将`Amoeba Plus`作为`localhost:3306/test`数据库的代理，所以需确保本地数据库存在test数据库，且已经启动。
- 修改conf/dbServers.xml，根据实际情况替换数据库的用户名和密码
- 下载`Amoeba Plus For MySQL`二进制包: [tar.gz](https://github.com/vispractice/Amoeba-Plus-For-MySQL/blob/master/release/amoeba-plus-mysql-1.0-RC1.tar.gz?raw=true  "tar.gz") | [zip](https://github.com/vispractice/Amoeba-Plus-For-MySQL/blob/master/release/amoeba-plus-mysql-1.0-RC1.zip?raw=true "zip")
- 启动  
  > startup.bat(For Windows)  
  $ ./startup.sh(For Linux)

**Tips:** 如需更多配置，可以参考[Amoeba使用指南](http://docs.hexnova.com/amoeba/ "Amoeba使用指南")。Amoba Plus的配置相对之前做了一些细微调整，但是切分规则和读写分离规则都不变。

## 如何构建 ##

从源码构建也很简单：
- 安装JDK和Ant
- git clone git@github.com:vispractice/Amoeba-Plus-For-MySQL.git
- cd Amoeba-Plus-For-MySQL/com.vispractice.amoeba.base
- ant
- cd ..

就会看到名字如`amoeba-plus-mysql-release-${timestamp}`的文件夹。

## 待完善  ##
- 考虑实现一个虚拟的defaultPool，替代使用真实数据库
- 错误信息应该更加友好，现在有些错误是直接后端的异常，错误信息和错误码也没有规范
- 在开启分布式事务时，`Amoeba Plus`既是一个代理，也兼任事务协调者的角色，一旦宕机，事务参与者等状  态信息全部丢失，使追溯变得非常困难，可以考虑将事务协调的状态移到外部去
- `Amoeba Plus`自身的管理和监控
- 开发文档和使用文档

## LICENSE ##
[GNU GPL v3](http://www.gnu.org/licenses/gpl.html "GNU GPL v3")


