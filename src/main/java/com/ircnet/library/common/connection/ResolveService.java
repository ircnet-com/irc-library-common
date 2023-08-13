package com.ircnet.library.common.connection;

import com.ircnet.library.common.configuration.ServerModel;
import org.springframework.stereotype.Service;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class ResolveService {
    public InetAddress resolve(ServerModel serverModel) throws UnknownHostException {
        return resolve(serverModel.getAddress(), serverModel.getProtocol());
    }

    private InetAddress resolve(String hostname, ServerModel.Protocol protocol) throws UnknownHostException {
        InetAddress[] inetAddresses = InetAddress.getAllByName(hostname);
        List<Inet4Address> inet4Addresses = new ArrayList<>();
        List<Inet6Address> inet6Addresses = new ArrayList<>();

        if(inetAddresses.length == 0)
            throw new UnknownHostException();

        for(InetAddress inetAddress : inetAddresses) {
            if(inetAddress instanceof Inet6Address)
                inet6Addresses.add((Inet6Address) inetAddress);
            else
                inet4Addresses.add((Inet4Address) inetAddress);
        }

        if(protocol == ServerModel.Protocol.IPV6) {
            if(inet6Addresses.isEmpty())
                throw new UnknownHostException();

            return inet6Addresses.get(new Random().nextInt(inet6Addresses.size()));
        }

        else if(protocol == ServerModel.Protocol.IPV4) {
            if(inet4Addresses.isEmpty())
                throw new UnknownHostException();

            return inet4Addresses.get(new Random().nextInt(inet4Addresses.size()));
        }

        else {
            int index = new Random().nextInt(inetAddresses.length);
            return inetAddresses[index];
        }
    }

}
