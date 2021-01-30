# Nabiki e-Trade Infrastructure
**Release Version 1.2.0**

*Date:   1:29 pm., Jan. 17th, 2021
Author: Hongbao Chen <chenhongbao@outlook.com>*

## SPONSORS
Many thanks to the help of [JetBrains](https://www.jetbrains.com/) and its amazing products.
<p>
<a href="https://www.jetbrains.com/?from=nabiki">
<img src="https://ftp.bmp.ovh/imgs/2021/01/5447e9b99abef1d5.png" width="100"/>
</a>
</p>

## LICENSE
This software is licensed under [GNU AFFERO GENERAL PUBLIC LICENSE Version 3](https://www.gnu.org/licenses/agpl-3.0.en.html).

It is all-in-one trading platform for China's derivative market on CTP brokerage system. The software depends on [ctp4j](https://github.com/chenhongbao/ctp4j) which is a port of the [C++ SDK](http://www.sfit.com.cn/) to Java.

## USAGE
If you are maven user, just run the pom.xml. The dependency above has been setup and run goal:
```
dependency:copy-dependencies
```
to extract it into your folder. Run the ```nabiki-centre-[VERSION].jar``` and then connect with ```nabiki-client-[VERSION].jar```.
## Development
The client contains raw SDK, trading API and UI portal, and all the three methods can do your trade via the platform.

There's no tutorial currently. But if you need indeed, please contact the author via [e-mail](chenhongbao@outlook.com).
## CPT4N - .NET Wrapper for CTP SDK.
The wrapper provides access to CTP API in C# or other .NET language. The project also includes a sample use case for the API.

The [ctp4n](https://github.com/chenhongbao/ctp4n) contains a pre-built binary or you can build your own with the swig script
provided in source.
