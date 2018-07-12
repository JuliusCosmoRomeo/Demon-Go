import argparse
import sqlite3


def print_all(c):
    for row in c.execute('SELECT * FROM data'):
        print(row)


def clear_table(c):
    c.execute('DELETE FROM data')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Hmm')
    parser.add_argument('command')
    args = parser.parse_args()

    conn = sqlite3.connect("collected_data.db")
    c = conn.cursor()

    if args.command == 'all':
        print_all(c)
    elif args.command == 'clear':
        clear_table(c)

    conn.commit()
    conn.close()
