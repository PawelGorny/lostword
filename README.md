# LostWord
Tool for finding missing word for BIP39 seed, having (n-1) known ordered words.
Program works with BIP141/P2WPKH or BIP32/P2PKH Derivation Path.

Usage:
`java -jar lostWord.jar configurationFile`

If your problem cannot be covered by any of modes below, please contact me, I will try to modify program accordingly or help you find a seed.

How to use it
-------------
Please check files in /examples/ folder to see how to set up the configuration file.
Configuration file expects: address, number of words, known words and additionally derivation path. If not specified, the default will be used (m/0/0).
This version checks only one address - for the given path. In the future (or if requested) I will add possibility to verify all the addresses up to address number x. Today, if you know address but you do not know if it was first or second from the derivation path, you must launch program twice, using two different paths (m/0/0 and m/0/1).
It is possible to launch tests against 'hardened' addresses, using ' (apostrophe) as the last character of path.
Using page https://iancoleman.io/bip39/ you may easily check what to expect for the given seed.
By default program uses P2PKH script semantics for addresses like "1..." and P2WPKH for addresses like "bc1...".

Program could be launched in 2 modes:
<ol>
<li>ONE_UNKNOWN</li>
Suitable for seeds where we know the number of words, we know order of words, but we do not know one word.
Program checks seeds testing a 'lost' word on each position.
Configuration file example (seed with 6 words, one word missing):

    ONE_UNKNOWN
    1AcuLxsQSMTi6fLEbJ6F6sNsZ4NyqnUNSo
    6
    brother
    medal
    remove
    pitch
    hill

<li>KNOWN_POSITION</li>
Suitable for seeds where we know the number of words, we know order of words, we do not know one or more words, but we know position of lost word(s).
Program checks seeds testing a 'lost' word(s) on specified position(s).
Configuration file example (seed with 6 words, three first words are missing):

    KNOWN_POSITION
    bc1q8ctl93aqztw8z3jsfhzcl0hlukq9pc7jclmmt0
    6
    ?
    ?
    ?
    home
    car
    test
</ol>

Contact
-------
Contact email: crypto@pawelgorny.com
If you found this program useful, consider making a donation, I will appreciate it! 
**BTC**: `34dEiyShGJcnGAg2jWhcoDDRxpennSZxg8`

TODO
----
<ol>
<li>checking several addresses for the path (up to address number X)</li>
<li>add support for BIP44, BIP49, BIP84, BIP141</li>
</ol>