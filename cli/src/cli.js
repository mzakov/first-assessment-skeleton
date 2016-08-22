import vorpal from 'vorpal'
import { words } from 'lodash'
import { indexOf } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'

export const cli = vorpal()

let username
let server
let commands = ['disconnect', 'echo', 'broadcast', 'users']
let defCommand = undefined

cli
  .delimiter(cli.chalk['yellow']('ftd~$'))

cli
  .mode('connect <username> <host>')
  .delimiter(cli.chalk['green']('connected>'))
  .init(function (args, callback) {
    username = args.username
    server = connect({ host: args.host, port: 8080 }, () => {
      server.write(new Message({ username, command: 'connect' }).toJSON() + '\n')
      callback()
    })

    server.on('data', (buffer) => {
      this.log(Message.fromJSON(buffer).toString())
    })

    server.on('end', () => {
      cli.exec('exit')
    })
  })
  .action(function (input, callback) {
    const [ command, ...rest ] = words(input, /[^, ]+/g)
    const contents = rest.join(' ')

    if ((commands.indexOf(command) === -1 || command.substring(0, 1) !== '@') && defCommand !== undefined) {
      command = defCommand
    }

    if (command === 'disconnect') {
      server.end(new Message({ username, command }).toJSON() + '\n')
      defCommand = command
    } else if (command === 'echo') {
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
      defCommand = command
    } else if (command === 'broadcast') {
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
      defCommand = command
    } else if (command === 'users') {
      server.write(new Message({ username, command }).toJSON() + '\n')
      defCommand = command
    } else if (command.substring(0, 1) === '@') {
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
      defCommand = command
    } else {
      this.log(`Command <${command}> was not recognized`)
    }

    callback()
  })
