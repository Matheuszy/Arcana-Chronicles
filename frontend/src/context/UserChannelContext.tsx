import { ReactNode, useEffect } from 'react';
import { userChannelService } from '../services/chatService';
import { UserChannelMessage } from '../types/userChannel';
import { useSession } from './SessionContext';

export function UserChannelProvider({ children }: { children: ReactNode }) {
  const { userId, token } = useSession();

  useEffect(() => {
    if (userId === null || !token) {
      userChannelService.disconnect();
      return;
    }
    userChannelService.connect(token);
  }, [userId, token]);

  useEffect(() => () => userChannelService.disconnect(), []);

  return <>{children}</>;
}

export function useUserChannel(listener: (message: UserChannelMessage) => void): void {
  useEffect(() => userChannelService.onMessage(listener), [listener]);
}
