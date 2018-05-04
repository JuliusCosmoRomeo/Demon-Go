import socket

address = ("127.0.0.1", 8000)

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.connect(address)
    s.sendall(b" Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aenean pretium justo vitae tincidunt tincidunt. Nam sed nisi quis justo aliquam scelerisque. Mauris ac mollis est. Praesent eget enim leo. Mauris maximus ligula bibendum metus faucibus, vel faucibus nisl ullamcorper. Pellentesque eu velit eu nisl maximus imperdiet. Aenean facilisis, tellus in laoreet vestibulum, erat nisi imperdiet tortor, sit amet condimentum dolor turpis viverra elit. In arcu sapien, sollicitudin sit amet tortor in, ultricies pretium nunc. \n Nam efficitur enim sit amet tellus tristique pretium. Morbi risus lacus, tincidunt tempus sollicitudin eget, pellentesque vel sem. Phasellus non nisi sollicitudin, mollis lorem vel, sodales lacus. Duis et enim et dolor aliquam pharetra. Mauris at justo a diam luctus consectetur. Aenean vel nisi id odio pharetra commodo eu in orci. Nulla auctor, nulla sit amet tempor luctus, sem nulla maximus nunc, at lobortis tellus elit posuere tortor. Nulla pretium pellentesque orci in ultricies. Curabitur tempor in lorem lobortis rutrum. Curabitur vestibulum ac diam id euismod. Suspendisse ut elementum leo. Integer vel sem vel augue bibendum maximus non id enim. Integer eget rhoncus nulla. Etiam sagittis, felis a commodo vulputate, orci massa vehicula ligula, a imperdiet tortor nisl non dui.\nNullam malesuada dictum nisl quis dignissim. Mauris varius, velit at dapibus lobortis, ipsum diam eleifend dui, at bibendum mi nunc quis magna. Morbi neque metus, feugiat a justo vel, scelerisque ullamcorper ligula. Nulla gravida tortor non cursus ornare. Phasellus efficitur vulputate arcu, nec venenatis sapien faucibus ac. Curabitur fringilla mi sed nunc accumsan, quis accumsan quam finibus. Phasellus tempus nisi nec nibh fringilla sollicitudin. Morbi vitae risus feugiat lorem mollis euismod. Pellentesque vel congue metus. In hac habitasse platea dictumst. Nam ullamcorper varius porta. Ut at odio a tortor pretium volutpat. ")
