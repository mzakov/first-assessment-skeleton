import vorpal from 'vorpal'
import { words } from 'lodash'
import { indexOf } from 'lodash'
import { split } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'

export const cli = vorpal()

let username
let server
let defCommand = ''

cli
  .delimiter(cli.chalk['yellow']('ftd~$'))
cli
  .mode('connect <username> <host>')
  .delimiter(cli.chalk['green'](':'))
  .init(function (args, callback) {
    username = args.username
    server = connect({ host: args.host, port: 8080 }, () => {
      server.write(new Message({ username, command: 'connect' }).toJSON() + '\n')
      callback()
    })

    cli
      .delimiter(cli.chalk['green'](username))
    server.on('data', (buffer) => {
      let message = Message.fromJSON(buffer)
      switch (message.command) {
        case 'echo':
          this.log(cli.chalk['green'](message.toString()))
          break
        case 'broadcast':
          this.log(cli.chalk['cyan'](message.toString()))
          break
        case 'connect':
          this.log(cli.chalk['gray'](message.toString()))
          break
        case 'disconnect':
          this.log(cli.chalk['gray'](message.toString()))
          break
        case 'users':
          this.log(cli.chalk['gray'](message.toString()))
          break
        default:
          this.log(message.toString())
      }
    })

    server.on('end', () => {
      cli.exec('exit')
    })
  })
  .action(function (input, callback) {
    const commands = ['disconnect', 'echo', 'broadcast', 'users']
    let cont
    let comm = split(input, ' ', 1)[0]

    if (indexOf(commands, comm, [0]) < 0 && comm.substring(0, 1) !== '@') {
      cont = defCommand + ' ' + input
    } else {
      cont = input
    }
    const [ command, ...rest ] = words(cont, /[^, ]+/g)
    const contents = rest.join(' ')
    if (command === 'disconnect') {
      server.end(new Message({ username, command }).toJSON() + '\n')
    } else if (command === 'echo') {
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
      defCommand = command
    } else if (command === 'broadcast') {
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
      defCommand = command
    } else if (command === 'users') {
      server.write(new Message({ username, command }).toJSON() + '\n')
      defCommand = command
    } else if (command.substring(0, 1) === '@' && command.length > 1) {
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
      defCommand = command
    } else {
      this.log(`Command <${command}> was not recognized`)
    }

    callback()
  })
